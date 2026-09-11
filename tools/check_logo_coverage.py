#!/usr/bin/env python3
"""Verify deterministic bundled-logo coverage for all 241 RMP records."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image

from build_logo_map import expected_mapping


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "restaurants.json"
MAP = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logo-map.json"
STANDARD = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logos"
CUSTOM = ROOT / "app" / "src" / "main" / "assets" / "custom-restaurant-logos"
CUSTOM_EVIDENCE = (
    ROOT / "data" / "source" / "generated-custom-logo-evidence-2026-09-11.json",
    ROOT / "data" / "source" / "generated-website-custom-logo-evidence-2026-09-11.json",
)
EXPECTED_RECORDS = 241


def load_map() -> dict[str, str]:
    duplicates: list[str] = []

    def hook(pairs: list[tuple[str, object]]) -> dict[str, object]:
        obj: dict[str, object] = {}
        for key, value in pairs:
            if key in obj:
                duplicates.append(key)
            obj[key] = value
        return obj

    raw = json.loads(MAP.read_text(), object_pairs_hook=hook)
    if duplicates:
        raise ValueError(f"duplicate map key(s): {', '.join(sorted(set(duplicates)))}")
    if not isinstance(raw, dict):
        raise ValueError("logo map root must be an object")
    mapping: dict[str, str] = {}
    for key, value in raw.items():
        if not isinstance(key, str) or not key:
            raise ValueError("logo map contains a blank/non-string RMP key")
        if not isinstance(value, str) or not value.endswith(".png") or "/" in value or "\\" in value:
            raise ValueError(f"unsafe logo filename for {key}: {value!r}")
        mapping[key] = value
    return mapping


def verify_custom_evidence() -> set[str]:
    documented: set[str] = set()
    documented_keys: set[str] = set()
    for evidence_path in CUSTOM_EVIDENCE:
        evidence = json.loads(evidence_path.read_text())
        records = evidence["records"]
        if evidence.get("recordCount") != len(records):
            raise ValueError(f"{evidence_path.name}: recordCount mismatch")
        file_assets: set[str] = set()
        for record in records:
            key = record["rmpKey"]
            if key in documented_keys:
                raise ValueError(f"duplicate custom evidence for {key}")
            documented_keys.add(key)
            relative = Path(record["generatedAssetPath"])
            expected_parent = Path("app/src/main/assets/custom-restaurant-logos")
            if relative.parent != expected_parent:
                raise ValueError(f"{key}: custom evidence points outside bundled custom-logo directory")
            path = ROOT / relative
            if not path.exists():
                raise ValueError(f"{key}: documented custom asset is missing: {relative.name}")
            with Image.open(path) as image:
                image.load()
                if image.size != (record["width"], record["height"]):
                    raise ValueError(f"{relative.name}: evidence dimensions mismatch")
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            if digest != record["sha256"]:
                raise ValueError(f"{relative.name}: evidence sha256 mismatch")
            documented.add(relative.name)
            file_assets.add(relative.name)
        if "assetCount" in evidence and evidence["assetCount"] != len(file_assets):
            raise ValueError(f"{evidence_path.name}: assetCount mismatch")
    return documented


def main() -> int:
    rows = json.loads(DATA.read_text())
    if len(rows) != EXPECTED_RECORDS:
        raise ValueError(f"dataset has {len(rows)} records; expected {EXPECTED_RECORDS}")
    keys = [row["rmpKey"] for row in rows]
    if len(set(keys)) != len(keys):
        raise ValueError("dataset contains duplicate rmpKey values")

    documented_custom = verify_custom_evidence()
    present_custom = {path.name for path in CUSTOM.glob("*.png")}
    if documented_custom != present_custom:
        missing = sorted(documented_custom - present_custom)
        extra = sorted(present_custom - documented_custom)
        raise ValueError(f"custom evidence/file mismatch; missing={missing}, undocumented={extra}")

    mapping = load_map()
    if set(mapping) != set(keys):
        missing = sorted(set(keys) - set(mapping))
        extra = sorted(set(mapping) - set(keys))
        raise ValueError(f"logo map key mismatch; missing={missing}, extra={extra}")

    standard = {path.name for path in STANDARD.glob("*.png")}
    custom = present_custom
    for key, filename in mapping.items():
        locations = int(filename in standard) + int(filename in custom)
        if locations != 1:
            raise ValueError(f"{key}: mapped logo {filename!r} resolves to {locations} bundled files")

    expected = expected_mapping()
    if mapping != expected:
        mismatches = [key for key in sorted(set(mapping) | set(expected)) if mapping.get(key) != expected.get(key)]
        raise ValueError(f"logo map differs from evidence-derived mapping for {len(mismatches)} key(s): {mismatches}")

    print(
        f"logo coverage gate PASS: {len(mapping)}/{EXPECTED_RECORDS} RMP records resolve to bundled logos; "
        f"{len(standard)} restaurant marks + {len(custom)} custom marks",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
