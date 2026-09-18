#!/usr/bin/env python3
"""Freeze Google's one-line description of each restaurant into app assets.

Runs at build time only, after tools/build_photos.py has recorded each
restaurant's Google place id. Writes restaurant-blurb-map.json (rmpKey -> text).
The phone app never talks to Google.
"""

from __future__ import annotations

import json
import time
from pathlib import Path

import requests

from build_photos import EVIDENCE, ROOT, api_key

BLURB_MAP = ROOT / "app" / "src" / "main" / "assets" / "restaurant-blurb-map.json"
DETAILS_URL = "https://places.googleapis.com/v1/places/{place_id}"


def main() -> None:
    key = api_key()
    evidence = json.loads(EVIDENCE.read_text())
    blurbs = json.loads(BLURB_MAP.read_text()) if BLURB_MAP.exists() else {}
    session = requests.Session()
    found = missing = failed = 0
    for rmp_key, record in evidence.items():
        place_id = record.get("placeId")
        if not place_id or rmp_key in blurbs:
            continue
        try:
            resp = session.get(
                DETAILS_URL.format(place_id=place_id),
                headers={"X-Goog-Api-Key": key, "X-Goog-FieldMask": "editorialSummary,generativeSummary"},
                timeout=20,
            )
            resp.raise_for_status()
            body = resp.json()
            text = (body.get("editorialSummary") or {}).get("text") or ((body.get("generativeSummary") or {}).get("overview") or {}).get("text")
            if text:
                blurbs[rmp_key] = text.strip()
                found += 1
            else:
                missing += 1
        except Exception as exc:
            failed += 1
            print(f"  ! {rmp_key}: {exc}")
        time.sleep(0.1)
    BLURB_MAP.write_text(json.dumps(blurbs, indent=2, sort_keys=True, ensure_ascii=False) + "\n")
    print(f"blurbs: {found} new, {missing} without one, {failed} failed, {len(blurbs)} total")


if __name__ == "__main__":
    main()
