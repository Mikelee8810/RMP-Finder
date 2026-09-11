#!/usr/bin/env python3
"""Report what an APK actually contains.

Used by CI so a size or packaging claim is measured from the built artifact
rather than asserted. Optionally fails when the packaged ABIs are not the ones
the build is supposed to ship.
"""

from __future__ import annotations

import argparse
import collections
import zipfile
from pathlib import Path


def bucket(name: str) -> str:
    if name.startswith("lib/"):
        return "lib/" + name.split("/")[1]
    if name.startswith("assets/restaurant-logos"):
        return "assets/restaurant-logos/"
    if name.startswith("assets/"):
        return "assets/"
    if name.startswith("res/"):
        return "res/"
    if name.endswith(".dex"):
        return "*.dex"
    return name.split("/")[0]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument(
        "--expect-abis",
        default="",
        help="comma-separated ABIs the APK must contain exactly, e.g. arm64-v8a",
    )
    args = parser.parse_args()

    with zipfile.ZipFile(args.apk) as archive:
        entries = archive.infolist()

    total = sum(entry.compress_size for entry in entries)
    sizes: collections.Counter[str] = collections.Counter()
    for entry in entries:
        sizes[bucket(entry.filename)] += entry.compress_size

    abis = sorted({e.filename.split("/")[1] for e in entries if e.filename.startswith("lib/")})

    print(f"{args.apk.name}: {total / 1e6:.1f} MB")
    print(f"packaged ABIs: {', '.join(abis) if abis else 'none'}")
    print()
    print(f"{'component':32} {'MB':>8} {'share':>7}")
    for name, size in sizes.most_common(10):
        print(f"{name:32} {size / 1e6:8.2f} {100 * size / total:6.1f}%")

    if args.expect_abis:
        expected = sorted(a.strip() for a in args.expect_abis.split(",") if a.strip())
        if abis != expected:
            print()
            print(f"::error::Expected ABIs {expected} but the APK packages {abis}.")
            return 1
        print()
        print(f"ABI check passed: {expected}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
