#!/usr/bin/env python3
"""Freeze first-party restaurant website icons into offline app assets.

The runtime never needs to scrape a site. This script is only a curation tool: it
visits each distinct verified restaurant website domain, finds that site's icon,
stores a small local copy, and writes durable source evidence for the dataset.
"""

from __future__ import annotations

import hashlib
import io
import json
import re
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urljoin, urlparse

import requests
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
OUT = ROOT / "data" / "source" / "website-logo-discovery-2026-09-10.json"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logos"
CHECKED_AT = "2026-09-10"
MAX_BYTES = 2_000_000
TIMEOUT = 10
SOCIAL_DOMAINS = {"facebook.com", "instagram.com"}


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


def candidate_urls(session: requests.Session, website: str) -> tuple[str, list[str]]:
    response = session.get(website, timeout=TIMEOUT, allow_redirects=True)
    response.raise_for_status()
    if len(response.content) > MAX_BYTES:
        raise ValueError("homepage too large")
    parser = IconParser()
    parser.feed(response.text)
    base = response.url
    candidates = [urljoin(base, href) for _, href in sorted(parser.icons, reverse=True)]
    origin = f"{urlparse(base).scheme}://{urlparse(base).netloc}/"
    candidates.extend(
        urljoin(origin, path)
        for path in ("apple-touch-icon.png", "favicon-192x192.png", "favicon.png", "favicon.ico")
    )
    return base, list(dict.fromkeys(candidates))


def freeze_image(content: bytes, content_type: str, source_url: str, domain: str) -> dict[str, object] | None:
    content_type = content_type.split(";", 1)[0].strip().lower()
    looks_svg = content_type in {"image/svg+xml", "text/xml", "application/xml"} or source_url.lower().split("?")[0].endswith(".svg")
    asset_name = asset_name_for(domain)
    if looks_svg:
        # The Android UI intentionally has no SVG/network image dependency.
        # Skip SVG candidates and continue to the site's raster favicon fallbacks.
        return None

    try:
        with Image.open(io.BytesIO(content)) as image:
            image.load()
            width, height = image.size
            if width < 16 or height < 16:
                return None
            image.thumbnail((512, 512))
            if image.mode not in {"RGB", "RGBA"}:
                image = image.convert("RGBA")
            path = ASSETS / asset_name
            image.save(path, "PNG", optimize=True)
            frozen = path.read_bytes()
            return {
                "assetFile": path.name,
                "format": "png",
                "width": width,
                "height": height,
                "sha256": hashlib.sha256(frozen).hexdigest(),
            }
    except Exception:
        return None


def main() -> None:
    rows = json.loads(DATA.read_text())
    websites: dict[str, str] = {}
    for row in rows:
        website = row.get("website")
        if website:
            domain = domain_for(website)
            if domain not in SOCIAL_DOMAINS:
                websites.setdefault(domain, website)

    ASSETS.mkdir(parents=True, exist_ok=True)
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
            resolved_website, candidates = candidate_urls(session, website)
            result["resolvedWebsiteUrl"] = resolved_website
            errors: list[str] = []
            for candidate in candidates[:12]:
                try:
                    response = session.get(candidate, timeout=TIMEOUT, allow_redirects=True)
                    response.raise_for_status()
                    if not response.content or len(response.content) > MAX_BYTES:
                        continue
                    frozen = freeze_image(response.content, response.headers.get("content-type", ""), response.url, domain)
                    if frozen:
                        result.update(frozen)
                        result["imageSourceUrl"] = response.url
                        break
                except Exception as exc:
                    errors.append(type(exc).__name__)
            if result["assetFile"] is None:
                result["failure"] = "No usable site icon found" + (f" ({', '.join(sorted(set(errors)))})" if errors else "")
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
