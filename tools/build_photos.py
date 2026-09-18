#!/usr/bin/env python3
"""Freeze one storefront photo per restaurant into offline app assets.

Runs at build time only. For each restaurant it asks Google Places (New) for
the place by name + address, takes its first photo, downsizes it to a small
JPEG and writes it to the app's assets. The phone app never talks to Google;
it just reads the bundled file. Attribution Google requires is kept beside the
photo map so the app can show it.

Needs GOOGLE_PLACES_KEY in local.properties (or the environment). Photo calls
sit in Google's free monthly allowance for a list this size.
"""

from __future__ import annotations

import argparse
import io
import json
import os
import re
import sys
import time
from pathlib import Path

import requests
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "restaurant-photos"
PHOTO_MAP = ROOT / "app" / "src" / "main" / "assets" / "restaurant-photo-map.json"
EVIDENCE = ROOT / "data" / "source" / "places-photo-discovery.json"
SEARCH_URL = "https://places.googleapis.com/v1/places:searchText"
PHOTO_URL = "https://places.googleapis.com/v1/{name}/media"
MAX_WIDTH = 600
JPEG_QUALITY = 70
TIMEOUT = 20


def api_key() -> str:
    key = os.environ.get("GOOGLE_PLACES_KEY", "").strip()
    if not key:
        props = ROOT / "local.properties"
        if props.exists():
            for line in props.read_text().splitlines():
                if line.startswith("GOOGLE_PLACES_KEY="):
                    key = line.split("=", 1)[1].strip()
    if not key:
        sys.exit("GOOGLE_PLACES_KEY is not set (local.properties or environment).")
    return key


def filename_for(rmp_key: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", rmp_key.lower()).strip("-") + ".jpg"


def query_for(r: dict) -> str:
    name = r.get("currentName") or r["officialName"]
    addr = r.get("currentAddress") or r["officialAddress"]
    return f"{name}, {addr['line1']}, {addr['city']}, {addr['state']} {addr['zip']}"


def find_photo(session: requests.Session, key: str, r: dict) -> dict | None:
    resp = session.post(
        SEARCH_URL,
        headers={"X-Goog-Api-Key": key, "X-Goog-FieldMask": "places.id,places.photos"},
        json={"textQuery": query_for(r), "maxResultCount": 1},
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    places = resp.json().get("places") or []
    if not places or not places[0].get("photos"):
        return None
    photo = places[0]["photos"][0]
    return {
        "placeId": places[0]["id"],
        "photoName": photo["name"],
        "attributions": [a.get("displayName", "") for a in photo.get("authorAttributions", [])],
    }


def download(session: requests.Session, key: str, photo_name: str) -> bytes:
    resp = session.get(
        PHOTO_URL.format(name=photo_name),
        params={"maxWidthPx": MAX_WIDTH, "key": key},
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    return resp.content


def shrink(raw: bytes) -> bytes:
    image = ImageOps.exif_transpose(Image.open(io.BytesIO(raw))).convert("RGB")
    image.thumbnail((MAX_WIDTH, MAX_WIDTH))
    out = io.BytesIO()
    image.save(out, "JPEG", quality=JPEG_QUALITY, optimize=True, progressive=True)
    return out.getvalue()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=0, help="only the first N restaurants (for a dry run)")
    parser.add_argument("--refresh", action="store_true", help="re-fetch photos that already exist")
    args = parser.parse_args()

    key = api_key()
    restaurants = json.loads(DATA.read_text())
    if args.limit:
        restaurants = restaurants[: args.limit]
    ASSETS.mkdir(parents=True, exist_ok=True)
    photo_map = json.loads(PHOTO_MAP.read_text()) if PHOTO_MAP.exists() else {}
    evidence = json.loads(EVIDENCE.read_text()) if EVIDENCE.exists() else {}
    session = requests.Session()
    found = skipped = missing = failed = 0

    for r in restaurants:
        rmp_key = r["rmpKey"]
        filename = filename_for(rmp_key)
        if not args.refresh and (ASSETS / filename).exists():
            skipped += 1
            continue
        try:
            hit = find_photo(session, key, r)
            if hit is None:
                missing += 1
                evidence[rmp_key] = {"status": "no_photo", "checkedAt": time.strftime("%Y-%m-%d")}
                print(f"  - no photo   {r['officialName']}")
                continue
            (ASSETS / filename).write_bytes(shrink(download(session, key, hit["photoName"])))
            photo_map[rmp_key] = {"file": filename, "attribution": ", ".join(a for a in hit["attributions"] if a)}
            evidence[rmp_key] = {"status": "ok", "checkedAt": time.strftime("%Y-%m-%d"), **hit}
            found += 1
            print(f"  + saved      {r['officialName']}")
        except Exception as exc:  # keep going; one bad place must not stop the run
            failed += 1
            print(f"  ! failed     {r['officialName']}: {exc}")
        time.sleep(0.15)

    PHOTO_MAP.write_text(json.dumps(photo_map, indent=2, sort_keys=True) + "\n")
    EVIDENCE.parent.mkdir(parents=True, exist_ok=True)
    EVIDENCE.write_text(json.dumps(evidence, indent=2, sort_keys=True) + "\n")
    total = sum(f.stat().st_size for f in ASSETS.glob("*.jpg"))
    print(f"\nsaved {found}, already had {skipped}, no photo {missing}, failed {failed}; {len(list(ASSETS.glob('*.jpg')))} photos, {total / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
