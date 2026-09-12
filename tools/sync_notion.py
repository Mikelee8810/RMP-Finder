#!/usr/bin/env python3
"""Sync the restaurant directory from the Notion "RMP Restaurant Snapshot" database.

Notion is the ongoing editing workspace: add a row there to add a restaurant,
delete one to retire it, edit Hours/Phone/Website/Business Status to update
what the app shows. This script pulls the live database, merges it onto the
existing data/restaurants.json, and writes back geocodes Notion is missing so
each address is only ever looked up once.

Design constraints this respects (see docs/design-direction.md):
  - The OFFICIAL name/address (as OTDA published it) and the CURRENT business
    name/address are separate facts, and the app must never silently replace
    one with the other. Notion's Restaurant/Address/City/ZIP columns hold
    whatever is CURRENTLY true, so for an existing restaurant a difference
    from the already-recorded official identity becomes currentName /
    currentAddress. For a brand-new row (no rmpKey on file yet) there is no
    "official" record to diverge from, so the Notion values *are* official.
  - Removing an entry from Notion never silently drops it from the dataset:
    it is reported and requires --allow-removals to actually apply, the same
    guard the app itself enforces on every update.
  - businessStatus is only changed when the "Business Status" column is
    explicitly set; hours are only replaced when the "Hours" text parses
    cleanly. Anything ambiguous keeps the previously reviewed value rather
    than guessing.

Requires NOTION_TOKEN (an internal integration token shared with the
database) as an environment variable. Geocoding uses the free U.S. Census
one-line geocoder, so no key is needed for new addresses.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import re
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_data import parse_hours  # noqa: E402  (reuse the reviewed Hours-text parser)

# Some environments (this one included) ship without a working default trust
# store, which turns every HTTPS call into a silent, hard-to-diagnose
# CERTIFICATE_VERIFY_FAILED. Use certifi's bundle when it's installed; fall
# back to the platform default otherwise.
try:
    import certifi
    SSL_CONTEXT = ssl.create_default_context(cafile=certifi.where())
except ImportError:
    SSL_CONTEXT = ssl.create_default_context()

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "data/restaurants.json"
MANIFEST = ROOT / "data/manifest.json"
DATABASE_ID = "aa6af51c-2f79-494d-9a21-2667b1e53942"
NOTION_VERSION = "2022-06-28"
OTDA = "https://otda.ny.gov/programs/rmp/participating-restaurants/default.asp"
CENSUS_ONE_LINE = "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress"
TODAY = datetime.now(ZoneInfo("America/New_York")).date().isoformat()

VALID_BUSINESS_STATUS = {
    "likely_open", "likely_closed", "closed", "temporarily_closed",
    "rebranded", "moved", "conflicting", "unknown",
}


def notion_request(method: str, path: str, token: str, body: dict | None = None) -> dict:
    url = f"https://api.notion.com/v1/{path}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", f"Bearer {token}")
    req.add_header("Notion-Version", NOTION_VERSION)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30, context=SSL_CONTEXT) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"Notion API {method} {path} -> {e.code}: {e.read().decode()[:500]}") from e


def fetch_all_rows(token: str) -> list[dict]:
    rows: list[dict] = []
    cursor = None
    while True:
        body = {"page_size": 100}
        if cursor:
            body["start_cursor"] = cursor
        page = notion_request("POST", f"databases/{DATABASE_ID}/query", token, body)
        rows.extend(page["results"])
        if not page.get("has_more"):
            break
        cursor = page["next_cursor"]
    return rows


def prop_text(props: dict, name: str) -> str | None:
    p = props.get(name) or {}
    t = p.get("type")
    if t == "title":
        return "".join(x["plain_text"] for x in p["title"]).strip() or None
    if t == "rich_text":
        return "".join(x["plain_text"] for x in p["rich_text"]).strip() or None
    if t == "phone_number":
        return p.get("phone_number")
    if t == "url":
        return p.get("url")
    if t == "select":
        return (p.get("select") or {}).get("name")
    if t == "date":
        return (p.get("date") or {}).get("start")
    if t == "number":
        return p.get("number")
    return None


def parse_row(page: dict) -> dict:
    props = page["properties"]
    return {
        "page_id": page["id"],
        "rmpKey": prop_text(props, "RMP Key"),
        "restaurant": prop_text(props, "Restaurant"),
        "address": prop_text(props, "Address"),
        "city": prop_text(props, "City"),
        "zip": prop_text(props, "ZIP"),
        "area": prop_text(props, "Area"),
        "phone": prop_text(props, "Phone"),
        "website": prop_text(props, "Website"),
        "menuUrl": prop_text(props, "Menu URL"),
        "hours": prop_text(props, "Hours"),
        "hoursStatus": prop_text(props, "Hours Status"),
        "hoursVerified": prop_text(props, "Hours Verified"),
        "rmpVerified": prop_text(props, "RMP Verified"),
        "otdaSource": prop_text(props, "OTDA Source"),
        "businessStatus": prop_text(props, "Business Status"),
        "latitude": prop_text(props, "Latitude"),
        "longitude": prop_text(props, "Longitude"),
    }


def geocode_census(line1: str, city: str, zip_: str) -> dict | None:
    query = urllib.parse.urlencode({
        "address": f"{line1}, {city}, NY {zip_}",
        "benchmark": "Public_AR_Current",
        "format": "json",
    })
    try:
        with urllib.request.urlopen(f"{CENSUS_ONE_LINE}?{query}", timeout=20, context=SSL_CONTEXT) as resp:
            payload = json.loads(resp.read())
    except (urllib.error.URLError, TimeoutError):
        return None
    matches = ((payload.get("result") or {}).get("addressMatches")) or []
    if not matches:
        return None
    m = matches[0]
    return {
        "latitude": m["coordinates"]["y"],
        "longitude": m["coordinates"]["x"],
        "matchedAddress": m.get("matchedAddress"),
    }


def write_notion_coords(token: str, page_id: str, lat: float, lon: float) -> None:
    notion_request("PATCH", f"pages/{page_id}", token, {
        "properties": {
            "Latitude": {"number": round(lat, 7)},
            "Longitude": {"number": round(lon, 7)},
        },
    })


def addr(line1: str, city: str, zip_: str) -> dict:
    return {"line1": line1, "city": city, "state": "NY", "zip": zip_}


def names_differ(a: str | None, b: str | None) -> bool:
    return bool(a) and bool(b) and a.strip().lower() != b.strip().lower()


def addrs_differ(a: dict | None, b: dict) -> bool:
    if not a:
        return False
    return (a["line1"].strip().lower(), a["zip"]) != (b["line1"].strip().lower(), b["zip"])


def build_records(rows: list[dict], existing: dict[str, dict], token: str, write_back: bool) -> tuple[list[dict], list[str], list[str]]:
    """Returns (records, new_keys, geocoded_keys)."""
    records: list[dict] = []
    new_keys: list[str] = []
    geocoded_keys: list[str] = []

    for row in rows:
        key = row["rmpKey"]
        if not key:
            continue  # a placeholder row with no key yet; nothing to sync
        prior = existing.get(key)
        notion_addr = addr(row["address"] or "", row["city"] or "", row["zip"] or "")

        if prior is None:
            # Brand new restaurant: Notion's values ARE the official record.
            official_name = row["restaurant"]
            official_addr = notion_addr
            current_name = None
            current_addr = None
            new_keys.append(key)
        else:
            official_name = prior["officialName"]
            official_addr = prior["officialAddress"]
            current_name = row["restaurant"] if names_differ(row["restaurant"], official_name) else None
            current_addr = notion_addr if addrs_differ(official_addr, notion_addr) and row["address"] else prior.get("currentAddress")

        # Coordinates: Notion's own value wins (a human or a prior sync
        # reviewed it); otherwise reuse what is already on file; otherwise
        # this is a genuinely new address and needs a fresh geocode.
        if row["latitude"] is not None and row["longitude"] is not None:
            coords = {
                "latitude": row["latitude"], "longitude": row["longitude"],
                "addressRole": "official_rmp", "provider": "Notion (reviewed)",
                "matchQuality": "manual", "precision": "reviewed",
                "checkedAt": TODAY, "matchedAddress": None,
            }
        elif prior is not None:
            coords = prior["coordinates"]
        else:
            result = geocode_census(official_addr["line1"], official_addr["city"], official_addr["zip"])
            if result is None:
                print(f"  ! could not geocode new restaurant {official_name!r} ({key}); skipping this row", file=sys.stderr)
                continue
            coords = {
                "latitude": result["latitude"], "longitude": result["longitude"],
                "addressRole": "official_rmp", "provider": "U.S. Census Geocoder",
                "matchQuality": "exact", "precision": "interpolated",
                "checkedAt": TODAY, "matchedAddress": result["matchedAddress"],
            }
            if write_back:
                write_notion_coords(token, row["page_id"], coords["latitude"], coords["longitude"])
                time.sleep(0.35)  # stay well under Notion's rate limit
            geocoded_keys.append(key)

        # Hours: only replace a known-good structured value with another one
        # the parser is confident about; an unparseable or blank cell keeps
        # whatever was last reviewed rather than erasing it.
        parsed_hours = parse_hours(row["hours"]) if row["hours"] else None
        if row["hoursStatus"] == "Unknown":
            hours, hours_status = None, "unknown"
        elif parsed_hours is not None:
            hours, hours_status = parsed_hours, "usable"
        elif row["hoursStatus"] == "Needs refresh":
            hours, hours_status = (prior["hours"] if prior else None), "stale"
        else:
            hours = prior["hours"] if prior else None
            hours_status = prior["hoursStatus"] if prior else "unknown"

        business_status = row["businessStatus"] if row["businessStatus"] in VALID_BUSINESS_STATUS else (
            prior["businessStatus"] if prior else "unknown"
        )

        record = {
            "rmpKey": key,
            "officialName": official_name,
            "currentName": current_name,
            "aliases": prior["aliases"] if prior else [],
            "officialAddress": official_addr,
            "currentAddress": current_addr,
            "borough": row["area"] or (prior["borough"] if prior else ""),
            "zip": official_addr["zip"],
            "coordinates": coords,
            "phone": row["phone"] or (prior["phone"] if prior else None),
            "website": row["website"] or (prior["website"] if prior else None),
            "menuUrl": row["menuUrl"] or (prior["menuUrl"] if prior else None),
            "imageUrl": prior["imageUrl"] if prior else None,
            "imageAttribution": prior["imageAttribution"] if prior else None,
            "businessStatus": business_status,
            "hoursStatus": hours_status,
            "hours": hours,
            "rmpVerifiedAt": row["rmpVerified"] or (prior["rmpVerifiedAt"] if prior else TODAY),
            "businessCheckedAt": row["hoursVerified"] or (prior["businessCheckedAt"] if prior else TODAY),
            "conflictFlags": prior["conflictFlags"] if prior else [],
            "sources": prior["sources"] if prior else [
                {"kind": "NYS OTDA Restaurant Meals Program", "role": "rmp_eligibility",
                 "url": row["otdaSource"] or OTDA, "checkedAt": row["rmpVerified"] or TODAY},
                {"kind": coords["provider"], "role": "coordinates", "url": None, "checkedAt": TODAY},
            ],
        }
        records.append(record)

    records.sort(key=lambda r: r["rmpKey"])
    return records, new_keys, geocoded_keys


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-removals", action="store_true",
                         help="Apply a smaller roster instead of just reporting it.")
    parser.add_argument("--dry-run", action="store_true",
                         help="Print the diff without writing any file or Notion cell.")
    args = parser.parse_args()

    token = os.environ.get("NOTION_TOKEN")
    if not token:
        print("NOTION_TOKEN is not set.", file=sys.stderr)
        return 2

    existing_list = json.loads(OUT.read_text())
    existing = {r["rmpKey"]: r for r in existing_list}

    print("Fetching the Notion database…")
    rows = [parse_row(p) for p in fetch_all_rows(token)]
    print(f"  {len(rows)} rows")

    records, new_keys, geocoded_keys = build_records(rows, existing, token, write_back=not args.dry_run)

    new_keys_set = {r["rmpKey"] for r in records}
    missing = sorted(set(existing) - new_keys_set)
    if missing and not args.allow_removals:
        print(f"\n{len(missing)} restaurant(s) are on file but no longer in Notion:")
        for key in missing:
            print(f"  - {existing[key]['officialName']}  ({key})")
        print("\nThey were kept. Re-run with --allow-removals to actually retire them.")
        # Keep them in the output rather than silently dropping the roster.
        records.extend(existing[k] for k in missing)
        records.sort(key=lambda r: r["rmpKey"])

    if new_keys:
        print(f"\n{len(new_keys)} new restaurant(s):")
        for key in new_keys:
            print(f"  + {next(r['officialName'] for r in records if r['rmpKey'] == key)}  ({key})")
    if geocoded_keys:
        print(f"\nGeocoded {len(geocoded_keys)} new address(es) via the U.S. Census geocoder and wrote them back to Notion.")

    changed = json.dumps(records, sort_keys=True) != json.dumps(existing_list, sort_keys=True)
    if not changed:
        print("\nNo changes.")
        return 0

    if args.dry_run:
        print(f"\n(dry run) {len(records)} records would be written; nothing was saved.")
        return 0

    OUT.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n")

    manifest = json.loads(MANIFEST.read_text())
    manifest["datasetVersion"] += 1
    manifest["generatedAt"] = datetime.now(ZoneInfo("America/New_York")).isoformat(timespec="seconds")
    manifest["recordCount"] = len(records)
    manifest["sha256"] = hashlib.sha256(OUT.read_bytes()).hexdigest()
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

    print(f"\nWrote {len(records)} records. Dataset version {manifest['datasetVersion']}.")

    if new_keys:
        print("\nGenerating a fallback logo mark for any new restaurant without a bundled logo…")
        subprocess.run([sys.executable, str(ROOT / "tools/generate_website_custom_logos.py")], check=True, cwd=ROOT)
        subprocess.run([sys.executable, str(ROOT / "tools/build_logo_map.py")], check=True, cwd=ROOT)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
