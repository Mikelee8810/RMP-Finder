#!/usr/bin/env python3
"""Structural verification gate for data/restaurants.json, run on every push
and after every Notion sync (tools/sync_notion.py).

Deliberately size-agnostic: the restaurant count changes as entries are added
or retired through Notion, so nothing here pins an exact count, an exact
first/last key, or a specific restaurant's facts. What it does check never
should change regardless of roster size: schema validity, key uniqueness,
manifest/file agreement, and the same open-now and truth-separation
invariants the app itself relies on (a business the sources call closed must
never be reported as having usable hours; an "exact" geocode match must
actually match the house number it claims to).

For the frozen point-in-time regression test against the original
241-restaurant launch baseline, see tools/verify_baseline_2026-09-09.py.
"""
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"

VALID_BUSINESS_STATUS = {
    "likely_open", "likely_closed", "closed", "temporarily_closed",
    "rebranded", "moved", "conflicting", "unknown",
}
VALID_HOURS_STATUS = {"usable", "partial", "stale", "conflicting", "unknown"}
CLOSED_LIKE = {"closed", "temporarily_closed", "moved", "conflicting"}


def load(path):
    with path.open() as handle:
        return json.load(handle)


def house_number(value):
    match = re.match(r"^[0-9]+(?:-[0-9]+)?[A-Z]?", value.upper().strip())
    return match.group(0) if match else None


def main():
    rows = load(DATA / "restaurants.json")
    manifest = load(DATA / "manifest.json")
    restaurant_schema = load(DATA / "restaurant.schema.json")
    manifest_schema = load(DATA / "manifest.schema.json")

    assert rows, "restaurants.json is empty"
    keys = [row["rmpKey"] for row in rows]
    assert len(keys) == len(set(keys)), f"duplicate rmpKey(s): {[k for k, n in Counter(keys).items() if n > 1]}"

    checker = FormatChecker()
    row_validator = Draft202012Validator(restaurant_schema, format_checker=checker)
    manifest_validator = Draft202012Validator(manifest_schema, format_checker=checker)
    row_errors = [
        (row.get("rmpKey", f"row {index}"), error.message)
        for index, row in enumerate(rows)
        for error in row_validator.iter_errors(row)
    ]
    manifest_errors = [error.message for error in manifest_validator.iter_errors(manifest)]
    assert not row_errors, row_errors[:10]
    assert not manifest_errors, manifest_errors[:10]

    for row in rows:
        key = row["rmpKey"]
        coordinate = row["coordinates"]
        assert coordinate["matchQuality"] != "unmatched", key
        if coordinate["matchQuality"] == "exact":
            matched = coordinate.get("matchedAddress") or ""
            assert house_number(row["officialAddress"]["line1"]) == house_number(matched), key

        assert row["businessStatus"] in VALID_BUSINESS_STATUS, (key, row["businessStatus"])
        assert row["hoursStatus"] in VALID_HOURS_STATUS, (key, row["hoursStatus"])

        # "Open now" is only ever computed from hours the app trusts, and a
        # business the sources already call closed/rebranded/conflicting must
        # never carry a hoursStatus that would let open-now say otherwise.
        if row["hoursStatus"] == "usable":
            assert row["hours"] is not None, key
        if row["businessStatus"] in CLOSED_LIKE:
            assert row["hoursStatus"] != "usable", key

        # The two facts the whole app is built to keep apart.
        if row["currentName"] is not None:
            assert row["currentName"] != row["officialName"], key
        if row["currentAddress"] is not None:
            assert row["currentAddress"] != row["officialAddress"], key

    digest = hashlib.sha256((DATA / "restaurants.json").read_bytes()).hexdigest()
    assert manifest["recordCount"] == len(rows), (manifest["recordCount"], len(rows))
    assert manifest["sha256"] == digest, "manifest sha256 does not match restaurants.json — regenerate the manifest"
    assert isinstance(manifest["datasetVersion"], int) and manifest["datasetVersion"] > 0
    assert manifest["schemaVersion"] == 1
    assert manifest["minimumAppVersion"] == 1
    assert manifest["datasetUrl"] == "https://raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/data/restaurants.json"
    assert manifest["programPolicy"]["rmpDiscountPercent"] == 10

    print(f"records={len(rows)} unique_keys={len(set(keys))} schema_errors=0 manifest_errors=0")
    print(f"qualities={dict(Counter(row['coordinates']['matchQuality'] for row in rows))}")
    print(f"usable_hours={sum(row['hoursStatus'] == 'usable' for row in rows)}")
    print(f"sha256={digest} manifest_match=PASS")
    print("schema=PASS uniqueness=PASS truth_separation=PASS open_now_inputs=PASS")


if __name__ == "__main__":
    main()
