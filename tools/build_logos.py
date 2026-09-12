#!/usr/bin/env python3
"""Freeze first-party restaurant website icons into offline app assets.

The runtime never needs to scrape a site. This script is only a curation tool: it
visits each distinct verified restaurant website domain, finds that site's icon,
stores a small local copy, and writes durable source evidence for the dataset.

Assets are held to a quality gate so the app never ships a blank or unusably
small mark. A restaurant with no usable icon renders the built-in initial and
category fallback instead, which reads better than an upscaled 16px favicon.

Requires network access to the restaurant websites. Run it where that is
available; `.github/workflows/refresh-logos.yml` runs it on GitHub Actions.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import re
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urljoin, urlparse

import requests
from PIL import Image

try:  # Optional: only needed for brands that publish their mark as SVG.
    import cairosvg
except Exception:  # pragma: no cover - absence simply disables SVG candidates.
    cairosvg = None


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
OUT = ROOT / "data" / "source" / "website-logo-discovery-2026-09-10.json"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logos"
CHECKED_AT = "2026-09-10"
MAX_BYTES = 2_000_000
# Some brand sites are slow to answer before they serve an icon at all.
TIMEOUT = 20
SOCIAL_DOMAINS = {"facebook.com", "instagram.com"}

# A site with no icon of its own often serves one belonging to the platform it
# runs on. That is a WordPress or Facebook mark, not restaurant branding, and it
# is worse than the built-in fallback because it misidentifies the restaurant.
PLATFORM_ICON_PATTERNS = (
    "/wp-includes/",
    "/wp-admin/images/",
    "w-logo-blue",
    "w-logo-gray",
)

# Quality gate. An icon below these thresholds looks worse in the app than the
# built-in fallback, so it is rejected rather than bundled.
# Marks are drawn at 88dp, so a 48px favicon blurs; the long edge must reach 96px.
MIN_SOURCE_DIMENSION = 96
MIN_DISTINCT_COLORS = 3
ALPHA_VISIBLE_THRESHOLD = 8

# Large chains serve their homepage icon inconsistently (bot walls, SVG-only
# marks, or a generic app icon). These first-party brand URLs are tried before
# the generic homepage discovery so the biggest RMP chains resolve reliably.
BRAND_ICON_CANDIDATES: dict[str, tuple[str, ...]] = {
    "mcdonalds.com": (
        "https://www.mcdonalds.com/content/dam/sites/usa/nfl/icons/arches-logo_108x108.jpg",
        "https://www.mcdonalds.com/etc/designs/mcdonalds/clientlibs/img/favicon-192.png",
    ),
    "popeyes.com": (
        "https://www.popeyes.com/favicon-192x192.png",
        "https://www.popeyes.com/apple-touch-icon.png",
        "https://www.popeyes.com/logo.svg",
    ),
    "kfc.com": (
        "https://www.kfc.com/apple-touch-icon.png",
        "https://www.kfc.com/favicon-192x192.png",
        "https://www.kfc.com/assets/images/kfc-logo.svg",
    ),
    "checkers.com": (
        "https://www.checkers.com/apple-touch-icon.png",
    ),
    "locations.goldenkrust.com": (
        "https://www.goldenkrust.com/apple-touch-icon.png",
    ),
}


class IconParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.icons: list[tuple[int, str]] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() != "link":
            return
        values = {key.lower(): (value or "") for key, value in attrs}
        rel = values.get("rel", "").lower()
        href = values.get("href", "").strip()
        if not href or "icon" not in rel:
            return
        sizes = values.get("sizes", "")
        size = max((int(v) for v in re.findall(r"(\d+)[xX]\d+", sizes)), default=0)
        priority = 300 if "apple-touch" in rel else 200 if "shortcut" not in rel else 100
        self.icons.append((priority + min(size, 512), href))


def domain_for(url: str) -> str:
    return urlparse(url).netloc.lower().removeprefix("www.")


def asset_name_for(domain: str) -> str:
    # Android assets keep arbitrary filenames, so preserving the website domain
    # gives the app a simple, deterministic lookup key without a generated map.
    safe = re.sub(r"[^a-z0-9.-]+", "-", domain.lower()).strip("-.")
    return f"{safe}.png"


def candidate_urls(session: requests.Session, website: str, domain: str) -> tuple[str, list[str]]:
    candidates = list(BRAND_ICON_CANDIDATES.get(domain, ()))
    base = website
    try:
        response = session.get(website, timeout=TIMEOUT, allow_redirects=True)
        response.raise_for_status()
        if len(response.content) > MAX_BYTES:
            raise ValueError("homepage too large")
        parser = IconParser()
        parser.feed(response.text)
        base = response.url
        candidates.extend(urljoin(base, href) for _, href in sorted(parser.icons, reverse=True))
        origin = f"{urlparse(base).scheme}://{urlparse(base).netloc}/"
        candidates.extend(
            urljoin(origin, path)
            for path in ("apple-touch-icon.png", "favicon-192x192.png", "favicon.png", "favicon.ico")
        )
    except Exception:
        # A brand with explicit first-party candidates is still worth trying even
        # when its homepage blocks automated requests.
        if not candidates:
            raise
    return base, list(dict.fromkeys(candidates))


def is_platform_icon(url: str) -> bool:
    """True when a URL points at a site platform's own mark rather than a brand's."""
    lowered = url.lower()
    return any(pattern in lowered for pattern in PLATFORM_ICON_PATTERNS)


def quality_failure(image: Image.Image) -> str | None:
    """Return why an icon is unusable in the app, or None when it is good."""
    rgba = image.convert("RGBA")
    # Pillow 12 renamed getdata(); accept either so the tool runs on older hosts.
    pixels = rgba.get_flattened_data() if hasattr(rgba, "get_flattened_data") else rgba.getdata()
    visible = [pixel for pixel in pixels if pixel[3] > ALPHA_VISIBLE_THRESHOLD]
    if not visible:
        return "icon is fully transparent"
    distinct = len({pixel for pixel in visible})
    if distinct < MIN_DISTINCT_COLORS:
        return f"icon has only {distinct} visible color(s)"
    return None


def decode_image(content: bytes, content_type: str, source_url: str) -> Image.Image | None:
    content_type = content_type.split(";", 1)[0].strip().lower()
    looks_svg = (
        content_type in {"image/svg+xml", "text/xml", "application/xml"}
        or source_url.lower().split("?")[0].endswith(".svg")
    )
    if looks_svg:
        # The Android UI intentionally has no SVG or network-image dependency, so
        # a vector mark is rasterized here at build time instead of at runtime.
        if cairosvg is None:
            return None
        content = cairosvg.svg2png(bytestring=content, output_width=512, output_height=512)
    image = Image.open(io.BytesIO(content))
    image.load()
    return image


def freeze_image(content: bytes, content_type: str, source_url: str, domain: str) -> dict[str, object] | str:
    """Save a usable icon and describe it, or return the reason it was rejected."""
    try:
        image = decode_image(content, content_type, source_url)
    except Exception as exc:
        return f"{type(exc).__name__} while decoding"
    if image is None:
        return "vector icon skipped (cairosvg unavailable)"

    with image:
        width, height = image.size
        if max(width, height) < MIN_SOURCE_DIMENSION:
            return f"icon is {width}x{height}, below the {MIN_SOURCE_DIMENSION}px minimum"
        image.thumbnail((512, 512))
        if image.mode not in {"RGB", "RGBA"}:
            image = image.convert("RGBA")
        failure = quality_failure(image)
        if failure:
            return failure
        path = ASSETS / asset_name_for(domain)
        image.save(path, "PNG", optimize=True)
        frozen = path.read_bytes()
        return {
            "assetFile": path.name,
            "format": "png",
            "width": width,
            "height": height,
            "sha256": hashlib.sha256(frozen).hexdigest(),
        }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--fresh",
        action="store_true",
        help="delete every bundled asset first instead of keeping icons this run cannot re-fetch",
    )
    args = parser.parse_args()

    rows = json.loads(DATA.read_text())
    websites: dict[str, str] = {}
    for row in rows:
        website = row.get("website")
        if website:
            domain = domain_for(website)
            if domain not in SOCIAL_DOMAINS:
                websites.setdefault(domain, website)

    ASSETS.mkdir(parents=True, exist_ok=True)
    if args.fresh:
        for old in ASSETS.iterdir():
            if old.is_file():
                old.unlink()

    session = requests.Session()
    session.headers.update({"User-Agent": "Mozilla/5.0 (compatible; RMP-Finder-logo-curator/1.0)"})
    results: dict[str, dict[str, object]] = {}

    for index, (domain, website) in enumerate(sorted(websites.items()), start=1):
        result: dict[str, object] = {
            "websiteUrl": website,
            "checkedAt": CHECKED_AT,
            "assetFile": None,
            "credit": f"Website icon published by {domain}",
            "license": None,
        }
        try:
            resolved_website, candidates = candidate_urls(session, website, domain)
            result["resolvedWebsiteUrl"] = resolved_website
            rejections: list[str] = []
            for candidate in candidates[:12]:
                try:
                    response = session.get(candidate, timeout=TIMEOUT, allow_redirects=True)
                    response.raise_for_status()
                    if is_platform_icon(response.url):
                        rejections.append("platform icon, not restaurant branding")
                        continue
                    if not response.content or len(response.content) > MAX_BYTES:
                        continue
                    frozen = freeze_image(
                        response.content,
                        response.headers.get("content-type", ""),
                        response.url,
                        domain,
                    )
                    if isinstance(frozen, dict):
                        result.update(frozen)
                        result["imageSourceUrl"] = response.url
                        break
                    rejections.append(frozen)
                except Exception as exc:
                    rejections.append(type(exc).__name__)
            if result["assetFile"] is None:
                existing = ASSETS / asset_name_for(domain)
                detail = f" ({', '.join(sorted(set(rejections)))})" if rejections else ""
                if existing.exists():
                    # Keep the icon captured by an earlier run rather than losing
                    # coverage to a site that is merely unreachable right now.
                    result["assetFile"] = existing.name
                    result["retainedFromEarlierRun"] = True
                    result["sha256"] = hashlib.sha256(existing.read_bytes()).hexdigest()
                    result["failure"] = f"No usable site icon found this run{detail}; kept earlier asset"
                else:
                    result["failure"] = f"No usable site icon found{detail}"
        except Exception as exc:
            result["failure"] = f"{type(exc).__name__}: {exc}"

        results[domain] = result
        print(f"[{index:02d}/{len(websites)}] {domain}: {result.get('assetFile') or 'fallback'}")

    OUT.write_text(json.dumps(results, ensure_ascii=False, indent=2, sort_keys=True) + "\n")
    captured = sum(1 for value in results.values() if value.get("assetFile"))
    covered = sum(1 for row in rows if row.get("website") and results.get(domain_for(row["website"]), {}).get("assetFile"))
    print(f"captured_domains={captured}/{len(results)} covered_restaurants={covered}/{len(rows)} fallback_restaurants={len(rows)-covered}")


if __name__ == "__main__":
    main()
