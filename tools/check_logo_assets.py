#!/usr/bin/env python3
"""Guard the bundled restaurant logo assets.

Every bundled icon must pass the same quality gate the curator applies, and the
discovery evidence must describe exactly the set of files that ships in the APK.
Run by CI so an unusable or undocumented asset cannot reach a release.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_logos import MIN_SOURCE_DIMENSION, is_platform_icon, quality_failure  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "restaurant-logos"
EVIDENCE = ROOT / "data" / "source" / "website-logo-discovery-2026-09-10.json"


def main() -> int:
    failures: list[str] = []

    for path in sorted(ASSETS.iterdir()):
        if path.name.startswith("."):
            continue
        if path.suffix != ".png":
            failures.append(f"{path.name}: not a PNG")
            continue
        try:
            with Image.open(path) as image:
                image.load()
                width, height = image.size
                if width < MIN_SOURCE_DIMENSION or height < MIN_SOURCE_DIMENSION:
                    failures.append(f"{path.name}: {width}x{height} is below the {MIN_SOURCE_DIMENSION}px minimum")
                    continue
                reason = quality_failure(image)
                if reason:
                    failures.append(f"{path.name}: {reason}")
        except Exception as exc:
            failures.append(f"{path.name}: unreadable ({type(exc).__name__})")

    evidence = json.loads(EVIDENCE.read_text())
    claimed = {entry["assetFile"] for entry in evidence.values() if entry.get("assetFile")}
    present = {path.name for path in ASSETS.iterdir() if path.suffix == ".png"}

    # A platform's own mark misidentifies the restaurant, so it must not ship even
    # if it clears the size and colour thresholds.
    for domain, entry in sorted(evidence.items()):
        source = entry.get("imageSourceUrl")
        if entry.get("assetFile") and source and is_platform_icon(source):
            failures.append(f"{entry['assetFile']}: captured from a platform icon ({source})")

    for name in sorted(claimed - present):
        failures.append(f"{name}: claimed by the discovery evidence but not bundled")
    for name in sorted(present - claimed):
        failures.append(f"{name}: bundled but not recorded in the discovery evidence")

    if failures:
        print(f"logo asset gate FAILED ({len(failures)} problem(s)):")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print(f"logo asset gate PASS: {len(present)} assets, all above {MIN_SOURCE_DIMENSION}px and all documented")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
