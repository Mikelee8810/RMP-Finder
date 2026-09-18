#!/usr/bin/env python3
"""Push the build-time extras (photo, description, Google place id) into Notion.

The Notion database is the editing workspace for the restaurant list, so the
things the app now shows that Notion did not know about are written back here:
  - Photo:           the bundled storefront photo (served from the public repo)
  - Photo Credit:    who Google says took it
  - Description:     Google's one-line description
  - Google Place ID: so a re-fetch never has to search again

Adds the columns if they are missing, then updates every row by RMP Key.
Requires NOTION_TOKEN. Runs on GitHub Actions (.github/workflows/sync-notion.yml).
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path

from sync_notion import DATABASE_ID, fetch_all_rows, notion_request, prop_text

ROOT = Path(__file__).resolve().parents[1]
PHOTO_MAP = ROOT / "app/src/main/assets/restaurant-photo-map.json"
BLURB_MAP = ROOT / "app/src/main/assets/restaurant-blurb-map.json"
EVIDENCE = ROOT / "data/source/places-photo-discovery.json"
RAW_PHOTOS = "https://raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/app/src/main/assets/restaurant-photos/"

COLUMNS = {
    "Photo": {"files": {}},
    "Photo Credit": {"rich_text": {}},
    "Description": {"rich_text": {}},
    "Google Place ID": {"rich_text": {}},
}


def rich(text: str) -> dict:
    return {"rich_text": [{"text": {"content": text[:2000]}}]}


def main() -> int:
    token = os.environ.get("NOTION_TOKEN", "").strip()
    if not token:
        sys.exit("NOTION_TOKEN is not set.")
    photos = json.loads(PHOTO_MAP.read_text()) if PHOTO_MAP.exists() else {}
    blurbs = json.loads(BLURB_MAP.read_text()) if BLURB_MAP.exists() else {}
    evidence = json.loads(EVIDENCE.read_text()) if EVIDENCE.exists() else {}

    db = notion_request("GET", f"databases/{DATABASE_ID}", token)
    missing = {name: spec for name, spec in COLUMNS.items() if name not in db["properties"]}
    if missing:
        notion_request("PATCH", f"databases/{DATABASE_ID}", token, {"properties": missing})
        print(f"added columns: {', '.join(missing)}")

    updated = skipped = 0
    for page in fetch_all_rows(token):
        key = prop_text(page["properties"], "RMP Key")
        if not key:
            continue
        props: dict = {}
        photo = photos.get(key)
        if photo:
            props["Photo"] = {"files": [{"name": photo["file"], "external": {"url": RAW_PHOTOS + photo["file"]}}]}
            props["Photo Credit"] = rich(photo.get("attribution") or "")
        if key in blurbs:
            props["Description"] = rich(blurbs[key])
        place_id = (evidence.get(key) or {}).get("placeId")
        if place_id:
            props["Google Place ID"] = rich(place_id)
        if not props:
            skipped += 1
            continue
        notion_request("PATCH", f"pages/{page['id']}", token, {"properties": props})
        updated += 1
    print(f"Notion rows updated: {updated}, nothing to add: {skipped}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
