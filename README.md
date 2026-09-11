# RMP Finder

Personal Android-first finder for Mike's New York Restaurant Meals Program directory.

RMP Finder is built around a curated, versioned static directory rather than OTDA's live locator. Notion remains the enrichment/editing workspace; this repository contains the validated export consumed by the Android app.

## Current direction

- Android-first, personal-use app optimized for Mike's Pixel
- Offline-first restaurant data, search, filters, favorites, and saved status
- Jetpack Compose UI
- Room local database
- MapLibre map with clustered markers
- GitHub-hosted static dataset updates
- Transit directions handed off to the installed maps app
- No accounts, backend, social features, admin dashboard, or AI features in v1

## Current directory readiness

- 241 restaurants loaded
- 0 duplicate RMP keys
- 0 missing addresses or ZIPs
- 0 missing RMP verification dates
- 1 missing phone
- 15 locations with no source hour text
- 173 locations with structured hours usable for open-now checks
- 68 locations explicitly excluded from open-now checks because hours/status are partial, stale, conflicting, or unknown
- 47 missing websites

The incomplete fields are represented as explicit unknown or conflict states. They do not block the core finder.

## Repository status

The pre-build data gate and Android v1 acceptance gate are complete. The repository contains the validated 241-location export, manifest, source snapshot, geocoding evidence/review, schemas, generation script, verification script, Android app source, and acceptance tests.

Run `python3 tools/build_data.py` followed by `python3 tools/verify_data.py` to regenerate and verify the static dataset.

## Build and release

GitHub Actions builds every push and publishes signed release APKs. `Android CI`
runs the dataset gate, the logo asset gate, the unit tests and a debug build.
`Release APK` builds and signs the release, publishes it with its SHA-256, and
re-downloads the published file to confirm the GitHub-hosted copy matches. See
[build and release](docs/release.md) for the signing secrets and the exact steps.

## Install

Download the latest installable APK from [GitHub Releases](https://github.com/Mikelee8810/RMP-Finder/releases/latest). See [Android install instructions](docs/install.md) for the exact steps.

## Pre-code specification

- [Product requirements](docs/product-requirements.md)
- [Architecture decisions](docs/architecture-decisions.md)
- [Design direction](docs/design-direction.md)
- [Data readiness and build gate](docs/data-readiness.md)
- [Android v1 acceptance evidence](docs/android-v1-acceptance.md)
- [Android install instructions](docs/install.md)
- [Build and release](docs/release.md)
- [Restaurant record schema](data/restaurant.schema.json)
- [Dataset manifest schema](data/manifest.schema.json)
