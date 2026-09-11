#!/usr/bin/env python3
"""Build the exact RMP-key to bundled-logo map consumed by the Android app."""

from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
STANDARD_ASSETS = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logos"
CUSTOM_ASSETS = ROOT / "app" / "src" / "main" / "assets" / "custom-restaurant-logos"
MAP = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logo-map.json"
CUSTOM_EVIDENCE = (
    ROOT / "data" / "source" / "generated-custom-logo-evidence-2026-09-11.json",
    ROOT / "data" / "source" / "generated-website-custom-logo-evidence-2026-09-11.json",
)
STANDARD_DOMAIN_ALIASES = {
    "locations.kfc.com": "kfc.com.png",
}


def domain_for(url: str | None) -> str | None:
    if not url:
        return None
    return urlparse(url).netloc.lower().removeprefix("www.") or None


def custom_mapping() -> dict[str, str]:
    mapping: dict[str, str] = {}
    for evidence_path in CUSTOM_EVIDENCE:
        evidence = json.loads(evidence_path.read_text())
        for record in evidence["records"]:
            filename = Path(record["generatedAssetPath"]).name
            previous = mapping.setdefault(record["rmpKey"], filename)
            if previous != filename:
                raise RuntimeError(f"Conflicting custom assets for {record['rmpKey']}")
    return mapping


def expected_mapping() -> dict[str, str]:
    rows = json.loads(DATA.read_text())
    standard = {path.name for path in STANDARD_ASSETS.glob("*.png")}
    custom = {path.name for path in CUSTOM_ASSETS.glob("*.png")}
    generated = custom_mapping()
    mapping: dict[str, str] = {}
    missing: list[str] = []

    for row in rows:
        key = row["rmpKey"]
        filename = generated.get(key)
        if filename is None:
            domain = domain_for(row.get("website"))
            if domain in STANDARD_DOMAIN_ALIASES:
                filename = STANDARD_DOMAIN_ALIASES[domain]
            elif domain and f"{domain}.png" in standard:
                filename = f"{domain}.png"

        if filename is None or filename not in standard | custom:
            missing.append(key)
        else:
            mapping[key] = filename

    if missing:
        raise RuntimeError(f"No bundled logo for {len(missing)} RMP record(s): " + ", ".join(missing))
    return mapping


def main() -> None:
    mapping = expected_mapping()
    MAP.write_text(json.dumps(dict(sorted(mapping.items())), ensure_ascii=False, indent=2) + "\n")
    print(f"wrote {MAP.relative_to(ROOT)} with {len(mapping)} RMP keys")


if __name__ == "__main__":
    main()
