#!/usr/bin/env python3
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

from jsonschema import Draft202012Validator, FormatChecker


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
SOURCE = DATA / "source"

EXPECTED_COUNT = 241
EXPECTED_FIRST = "bronx|a daughter and two sons|1807 archer street|10460"
EXPECTED_LAST = "westchester|unk's fish & chips|31 john street|10701"


def load(path):
    with path.open() as handle:
        return json.load(handle)


def house_number(value):
    match = re.match(r"^[0-9]+(?:-[0-9]+)?[A-Z]?", value.upper().strip())
    return match.group(0) if match else None


def main():
    rows = load(DATA / "restaurants.json")
    source = load(SOURCE / "notion-rmp-snapshot-2026-09-09.json")
    reviews = load(SOURCE / "geocode-review-2026-09-09.json")
    manifest = load(DATA / "manifest.json")
    restaurant_schema = load(DATA / "restaurant.schema.json")
    manifest_schema = load(DATA / "manifest.schema.json")

    assert len(source) == EXPECTED_COUNT
    assert len({row["RMP Key"] for row in source}) == EXPECTED_COUNT
    assert len(rows) == EXPECTED_COUNT
    assert len({row["rmpKey"] for row in rows}) == EXPECTED_COUNT
    assert rows[0]["rmpKey"] == EXPECTED_FIRST
    assert rows[-1]["rmpKey"] == EXPECTED_LAST

    checker = FormatChecker()
    row_validator = Draft202012Validator(restaurant_schema, format_checker=checker)
    manifest_validator = Draft202012Validator(manifest_schema, format_checker=checker)
    row_errors = [
        (index, error.message)
        for index, row in enumerate(rows)
        for error in row_validator.iter_errors(row)
    ]
    manifest_errors = [error.message for error in manifest_validator.iter_errors(manifest)]
    assert not row_errors, row_errors[:10]
    assert not manifest_errors, manifest_errors[:10]

    assert len(reviews) == EXPECTED_COUNT
    assert len({review["rmpKey"] for review in reviews}) == EXPECTED_COUNT
    review_by_key = {review["rmpKey"]: review for review in reviews}

    for row in rows:
        coordinate = row["coordinates"]
        assert coordinate["matchQuality"] != "unmatched", row["rmpKey"]
        assert row["rmpKey"] in review_by_key
        assert review_by_key[row["rmpKey"]]["review"].strip()
        if coordinate["matchQuality"] == "exact":
            matched = coordinate.get("matchedAddress") or ""
            assert house_number(row["officialAddress"]["line1"]) == house_number(matched), row["rmpKey"]
        if row["hoursStatus"] == "usable":
            assert row["hours"] is not None, row["rmpKey"]

    expected_high_risk = {
        "bronx|dunkin donuts|2370 grand concourse road|10458": (
            "2370 Grand Concourse Road",
            "2366 Grand Concourse",
        ),
        "brooklyn|carnation restaurant|2310 86th street|11214": (
            "2310 86th Street",
            "2322 86th Street",
        ),
        "brooklyn|momo's mediterranean grill|25 101 avenue|11208": (
            "25 101 Avenue",
            "25 101st Avenue",
        ),
        "manhattan|el nuevo capri restaurant|1340 saint nicholas avenue|10033": (
            "1340 Saint Nicholas Avenue",
            "1342 Saint Nicholas Avenue",
        ),
        "queens|burger king|6259 fresh pond road|11365": (
            "6259 Fresh Pond Road",
            "62-59 Fresh Pond Road",
        ),
        "queens|mcdonald's|106-15 71st street|11375": (
            "106-15 71st Street",
            "106-15 71st Avenue",
        ),
        "queens|mcdonald's|22-14 31st avenue|11105": (
            "22-14 31st Avenue",
            "22-50 31st Street",
        ),
        "queens|mcdonald's|75-50 101st street|11416": (
            "75-50 101st Street",
            "75-50 101st Avenue",
        ),
        "staten island|justino's pizzeria|89 guyon street|10306": (
            "89 Guyon Street",
            "89 Guyon Avenue",
        ),
    }
    rows_by_key = {row["rmpKey"]: row for row in rows}
    for key, (official_line, current_line) in expected_high_risk.items():
        row = rows_by_key[key]
        assert row["officialAddress"]["line1"] == official_line
        assert row["currentAddress"]["line1"] == current_line
        assert "address_mismatch" in row["conflictFlags"]

    kings_plaza = rows_by_key["brooklyn|popeye's|5100 kings plaza|11234"]
    assert kings_plaza["officialAddress"]["line1"] == "5100 Kings Plaza"
    assert kings_plaza["coordinates"]["matchedAddress"] == "5100 Kings Plaza, Brooklyn, NY 11234"
    assert kings_plaza["coordinates"]["matchQuality"] == "manual"

    digest = hashlib.sha256((DATA / "restaurants.json").read_bytes()).hexdigest()
    assert manifest["recordCount"] == EXPECTED_COUNT
    assert manifest["sha256"] == digest
    assert manifest["datasetVersion"] == 1
    assert manifest["schemaVersion"] == 1
    assert manifest["minimumAppVersion"] == 1
    assert manifest["datasetUrl"] == "https://raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/data/restaurants.json"
    assert manifest["programPolicy"]["rmpDiscountPercent"] == 10
    assert manifest["programPolicy"]["checkedAt"] == "2026-09-09"

    for row in rows:
        if row["businessStatus"] in {"closed", "temporarily_closed", "moved", "conflicting"}:
            assert row["hoursStatus"] != "usable", row["rmpKey"]

    print(f"records={len(rows)} unique_keys={len(rows_by_key)} schema_errors=0 manifest_errors=0")
    print(f"geocodes={len(reviews)} qualities={dict(Counter(row['coordinates']['matchQuality'] for row in rows))}")
    print(f"usable_hours={sum(row['hoursStatus'] == 'usable' for row in rows)} usable_hours_missing=0")
    print(f"sha256={digest} manifest_match=PASS")
    print("high_risk_truth_separation=PASS kings_plaza_coordinate=PASS manifest_policy=PASS open_now_inputs=PASS")


if __name__ == "__main__":
    main()
