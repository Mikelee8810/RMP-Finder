#!/usr/bin/env python3
"""Generate original bundled marks for website-backed records with no verified logo.

These are deliberately app artwork rather than copies of restaurant trademarks.
They close the offline logo-coverage gap when a first-party site is unavailable,
parked, transparent, or only exposes platform branding.
"""

from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path
from urllib.parse import urlparse

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
STANDARD_ASSETS = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logos"
CUSTOM_ASSETS = ROOT / "app" / "src" / "main" / "assets" / "custom-restaurant-logos"
NO_WEBSITE_EVIDENCE = ROOT / "data" / "source" / "generated-custom-logo-evidence-2026-09-11.json"
OUT = ROOT / "data" / "source" / "generated-website-custom-logo-evidence-2026-09-11.json"
FONT_BOLD = Path("/System/Library/Fonts/Supplemental/Arial Bold.ttf")
FONT_REGULAR = Path("/System/Library/Fonts/Supplemental/Arial.ttf")
SIZE = 512
CHECKED_AT = "2026-09-11"

# The KFC location page is the same brand as the already bundled kfc.com mark.
STANDARD_ALIASES = {
    "locations.kfc.com": "kfc.com.png",
}

BRAND_LABELS = {
    "fixuplate.com": "FIX-U-PLATE",
    "gyrokingny.com": "Gyro King",
}

RESEARCH_NOTES = {
    "109pizzeriany.com": "Site exposed generic ordering-platform pizza artwork rather than a restaurant mark.",
    "876caribbeaneatery.com": "Site exposed Vite/Bolt platform artwork rather than a restaurant mark.",
    "andysrestaurantnyc.com": "Site exposed Hostinger platform branding rather than a restaurant mark.",
    "elnuevocapri3restaurantny.com": "Candidate labeled as a logo is a storefront photograph, so it was rejected.",
    "facebook.com": "Only Facebook platform artwork was retrievable from the restaurant profile.",
    "instagram.com": "Only Instagram platform artwork was retrievable from the restaurant profile.",
    "irestaurantny.com": "Menusifu Transparent Logo candidate is fully transparent.",
    "nago416.com": "Menusifu Transparent Logo candidate is fully transparent.",
    "nyaminzjaminzrest.com": "Restaurant domain currently resolves to a parked DropCatch page.",
    "sanmiwagocuisine.com": "Candidate logo is FoodCourt platform branding, not the restaurant.",
    "sapoaranyc.com": "Restaurant domain currently redirects to an unrelated site.",
    "sunhongwong.com": "Menusifu Transparent Logo candidate is fully transparent.",
}


def domain_for(url: str | None) -> str | None:
    if not url:
        return None
    return urlparse(url).netloc.lower().removeprefix("www.") or None


def slug(value: str) -> str:
    result = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
    return result or "restaurant"


def filename_for(domain: str, label: str) -> str:
    digest = hashlib.sha256(domain.encode()).hexdigest()[:12]
    return f"custom-web-{slug(label)}-{digest}.png"


def category_for(name: str) -> str:
    lower = name.lower()
    checks = (
        (("pizza", "pizzeria"), "PIZZA"),
        (("jerk", "caribbean"), "CARIBBEAN"),
        (("gyro", "shawarma", "halal"), "GRILL"),
        (("seafood", "ocean", "fish"), "SEAFOOD"),
        (("café", "cafe", "coffee"), "CAFÉ"),
        (("noodle", "kitchen", "restaurant"), "RESTAURANT"),
    )
    for needles, label in checks:
        if any(needle in lower for needle in needles):
            return label
    return "RESTAURANT"


def accent_for(domain: str) -> tuple[int, int, int, int]:
    raw = hashlib.sha256(domain.encode()).digest()
    # Keep accents saturated enough to read on the transparent/black preview.
    return (80 + raw[0] % 150, 80 + raw[1] % 150, 80 + raw[2] % 150, 255)


def initials(label: str) -> str:
    words = [word for word in re.findall(r"[A-Za-z0-9]+", label) if word.lower() not in {"the", "and", "restaurant"}]
    if not words:
        return "R"
    if len(words) == 1:
        return words[0][:2].upper()
    return (words[0][0] + words[1][0]).upper()


def fit_font(draw: ImageDraw.ImageDraw, text: str, max_width: int, start: int, minimum: int) -> ImageFont.FreeTypeFont:
    size = start
    while size > minimum:
        font = ImageFont.truetype(str(FONT_BOLD), size)
        if draw.textbbox((0, 0), text, font=font, stroke_width=2)[2] <= max_width:
            return font
        size -= 2
    return ImageFont.truetype(str(FONT_BOLD), minimum)


def wrap_name(draw: ImageDraw.ImageDraw, text: str) -> tuple[list[str], ImageFont.FreeTypeFont]:
    for size in range(42, 25, -2):
        font = ImageFont.truetype(str(FONT_BOLD), size)
        words = text.split()
        lines: list[str] = []
        current = ""
        for word in words:
            candidate = f"{current} {word}".strip()
            if draw.textbbox((0, 0), candidate, font=font, stroke_width=2)[2] <= 450:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = word
        if current:
            lines.append(current)
        if len(lines) <= 2:
            return lines, font
    return [text], fit_font(draw, text, 450, 26, 20)


def render_mark(domain: str, label: str, path: Path) -> None:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    accent = accent_for(domain)

    # Original generic meal mark: plate + utensils + a brand-neutral monogram.
    draw.rounded_rectangle((118, 62, 394, 316), radius=70, fill=(255, 255, 255, 245), outline=accent, width=14)
    draw.ellipse((150, 96, 362, 278), fill=accent, outline=(255, 255, 255, 255), width=8)
    mono = initials(label)
    mono_font = fit_font(draw, mono, 160, 92, 56)
    draw.text((256, 187), mono, font=mono_font, anchor="mm", fill=(255, 255, 255, 255), stroke_width=3, stroke_fill=(0, 0, 0, 120))

    # Simple fork and spoon silhouettes reinforce that this is app-made food artwork.
    utensil = (32, 32, 32, 255)
    draw.rounded_rectangle((78, 120, 91, 294), radius=6, fill=utensil)
    for x in (68, 78, 88, 98):
        draw.rounded_rectangle((x, 80, x + 7, 144), radius=3, fill=utensil)
    draw.ellipse((419, 80, 454, 157), fill=utensil)
    draw.rounded_rectangle((430, 137, 442, 294), radius=6, fill=utensil)

    lines, name_font = wrap_name(draw, label)
    line_height = name_font.size + 7
    start_y = 382 - ((len(lines) - 1) * line_height) / 2
    for idx, line in enumerate(lines):
        draw.text(
            (256, start_y + idx * line_height),
            line,
            font=name_font,
            anchor="mm",
            fill=(255, 255, 255, 255),
            stroke_width=5,
            stroke_fill=(20, 20, 20, 255),
        )

    category = category_for(label)
    category_font = ImageFont.truetype(str(FONT_REGULAR), 20)
    draw.text((256, 477), category, font=category_font, anchor="mm", fill=(245, 245, 245, 255), stroke_width=3, stroke_fill=(20, 20, 20, 255))

    image.save(path, "PNG", optimize=True)


def main() -> None:
    rows = json.loads(DATA.read_text())
    no_website = json.loads(NO_WEBSITE_EVIDENCE.read_text())
    existing_custom_keys = {record["rmpKey"] for record in no_website["records"]}
    standard = {path.name for path in STANDARD_ASSETS.glob("*.png")}

    targets: list[dict[str, object]] = []
    for row in rows:
        if row["rmpKey"] in existing_custom_keys:
            continue
        domain = domain_for(row.get("website"))
        if domain and f"{domain}.png" in standard:
            continue
        if domain in STANDARD_ALIASES:
            continue
        targets.append(row)

    grouped: dict[str, list[dict[str, object]]] = {}
    for row in targets:
        domain = domain_for(row.get("website"))
        if not domain:
            raise RuntimeError(f"Unexpected website-less target: {row['rmpKey']}")
        grouped.setdefault(domain, []).append(row)

    CUSTOM_ASSETS.mkdir(parents=True, exist_ok=True)
    expected_assets: set[str] = set()
    output_records: list[dict[str, object]] = []

    for domain, domain_rows in sorted(grouped.items()):
        label = BRAND_LABELS.get(domain) or str(domain_rows[0].get("currentName") or domain_rows[0]["officialName"])
        filename = filename_for(domain, label)
        path = CUSTOM_ASSETS / filename
        render_mark(domain, label, path)
        expected_assets.add(filename)
        payload = path.read_bytes()
        digest = hashlib.sha256(payload).hexdigest()

        for row in domain_rows:
            output_records.append(
                {
                    "rmpKey": row["rmpKey"],
                    "restaurantName": row.get("currentName") or row["officialName"],
                    "officialName": row["officialName"],
                    "borough": row["borough"],
                    "website": row.get("website"),
                    "domain": domain,
                    "generatedAssetPath": str(path.relative_to(ROOT)),
                    "generationDate": CHECKED_AT,
                    "assetOrigin": "original_generated_mark",
                    "generationMethod": "Deterministic Pillow composition using generic food artwork, monogram, and dataset display name",
                    "designBasis": "Original app artwork; not represented as the restaurant's trademark or official logo",
                    "researchNote": RESEARCH_NOTES.get(domain, "No trustworthy first-party restaurant mark was verified during the 2026-09-11 research pass."),
                    "width": SIZE,
                    "height": SIZE,
                    "sha256": digest,
                }
            )

    evidence = {
        "schemaVersion": 1,
        "generatedAt": CHECKED_AT,
        "purpose": "Original bundled marks for website-backed RMP records where no trustworthy restaurant logo could be verified.",
        "generator": "tools/generate_website_custom_logos.py",
        "recordCount": len(output_records),
        "assetCount": len(expected_assets),
        "records": sorted(output_records, key=lambda record: record["rmpKey"]),
    }
    OUT.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n")
    print(f"generated {len(expected_assets)} original custom marks covering {len(output_records)} RMP records")


if __name__ == "__main__":
    main()
