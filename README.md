# RMP Finder

Personal Android-first finder for Mike's New York Restaurant Meals Program directory.

RMP Finder is built around a curated, versioned static directory rather than OTDA's live locator. Notion remains the enrichment/editing workspace; this repository will contain the validated export consumed by the Android app.

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
- 15 locations without usable hours
- 23 locations needing status or hours confirmation
- 47 missing websites

The incomplete fields are represented as explicit unknown or conflict states. They do not block the core finder.

## Repository status

The repository is currently the durable project home for the design contract, data contract, and implementation plan. Android source and the validated 241-location export will be added after the dataset and map-coordinate intake is complete.
