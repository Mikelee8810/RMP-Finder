#!/usr/bin/env python3
"""Verify every displayed service fact against Google cache or a reviewed override."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
CACHE = ROOT / "data" / "source" / "google-places-cache.json"
OVERRIDES = ROOT / "data" / "service-fact-overrides.json"

DIRECT_FIELDS = (
    "takeout", "dineIn", "restroom", "servesBreakfast", "servesLunch", "servesDinner",
)
ACCESSIBILITY_FIELDS = (
    "wheelchairAccessibleEntrance", "wheelchairAccessibleRestroom",
    "wheelchairAccessibleSeating", "wheelchairAccessibleParking",
)
PARKING_FIELDS = (
    "freeParkingLot", "paidParkingLot", "freeStreetParking", "paidStreetParking",
    "valetParking", "freeGarageParking", "paidGarageParking",
)
ALL_FIELDS = set(DIRECT_FIELDS + ACCESSIBILITY_FIELDS + ("hasParking",))


def cache_facts(place: dict) -> dict[str, bool]:
    facts = {field: place[field] for field in DIRECT_FIELDS if isinstance(place.get(field), bool)}
    accessibility = place.get("accessibilityOptions") or {}
    facts.update({field: accessibility[field] for field in ACCESSIBILITY_FIELDS
                  if isinstance(accessibility.get(field), bool)})
    parking = place.get("parkingOptions") or {}
    if any(isinstance(parking.get(field), bool) for field in PARKING_FIELDS):
        facts["hasParking"] = any(parking.get(field) is True for field in PARKING_FIELDS)
    return facts


def audit(rows: list[dict], cache: dict, overrides: dict) -> tuple[int, int]:
    keys = {row["rmpKey"] for row in rows}
    errors: list[str] = []
    compared = approved = 0

    for key, values in overrides.items():
        if key not in keys:
            errors.append(f"stale override for missing restaurant: {key}")
        unknown = set(values) - ALL_FIELDS - {"reason"}
        if unknown:
            errors.append(f"{key}: unknown override fields {sorted(unknown)}")
        if not values.get("reason"):
            errors.append(f"{key}: override needs a reason")

    for row in rows:
        key = row["rmpKey"]
        place = cache.get(key) or {}
        # Never compare a cleared or corrected listing with stale facts from a
        # known-bad cached Place ID.
        if not row.get("googlePlaceId") or row.get("googlePlaceId") != place.get("id"):
            continue
        expected_overrides = overrides.get(key, {})
        for field, google_value in cache_facts(place).items():
            compared += 1
            actual = row.get(field)
            if field in expected_overrides:
                approved += 1
                if actual != expected_overrides[field]:
                    errors.append(
                        f"{key}: {field}={actual!r}; reviewed value is {expected_overrides[field]!r}"
                    )
            elif actual != google_value:
                errors.append(
                    f"{key}: undocumented {field} mismatch (Google={google_value!r}, app={actual!r})"
                )

    if errors:
        raise AssertionError("\n".join(errors))
    return compared, approved


def main() -> None:
    rows = json.loads(DATA.read_text(encoding="utf-8"))
    cache = json.loads(CACHE.read_text(encoding="utf-8"))
    overrides = json.loads(OVERRIDES.read_text(encoding="utf-8"))
    compared, approved = audit(rows, cache, overrides)
    print(f"service_facts_compared={compared} reviewed_overrides={approved} result=PASS")


if __name__ == "__main__":
    main()
