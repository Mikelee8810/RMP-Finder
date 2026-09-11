#!/usr/bin/env python3
"""Verify the built APK contains the exact logo map and every mapped asset."""

from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MAP = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logo-map.json"
APK_MAP_ENTRY = "assets/restaurant-logo-map.json"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()

    source_mapping = json.loads(SOURCE_MAP.read_text())
    with zipfile.ZipFile(args.apk) as archive:
        names = set(archive.namelist())
        if APK_MAP_ENTRY not in names:
            raise ValueError(f"APK is missing {APK_MAP_ENTRY}")
        apk_mapping = json.loads(archive.read(APK_MAP_ENTRY))
        if apk_mapping != source_mapping:
            raise ValueError("APK logo map does not match the source logo map")

        missing: list[str] = []
        ambiguous: list[str] = []
        for key, filename in source_mapping.items():
            candidates = (
                f"assets/restaurant-logos/{filename}",
                f"assets/custom-restaurant-logos/{filename}",
            )
            present = [candidate for candidate in candidates if candidate in names]
            if not present:
                missing.append(f"{key} -> {filename}")
            elif len(present) > 1:
                ambiguous.append(f"{key} -> {filename}")
        if missing or ambiguous:
            raise ValueError(f"APK logo resolution failure; missing={missing}, ambiguous={ambiguous}")

    print(f"APK logo gate PASS: {len(source_mapping)}/241 mapped RMP records have bundled assets in {args.apk.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
