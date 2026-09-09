# RMP Finder — Architecture Decisions

These decisions are locked for the first implementation unless testing exposes a concrete problem.

## ADR-001: Native Android and local-first reads

- Kotlin and Jetpack Compose.
- Room is the runtime source of truth.
- The bundled `restaurants.json` is validated and imported on first launch.
- UI reads Room through repositories and ViewModels using Flow/StateFlow.

Reason: Android's offline-first guidance recommends a local source for critical reads. JSON is easier than a prebuilt Room file to regenerate, inspect, validate, and diff for 241 records.

## ADR-002: Static GitHub update channel

- Latest manifest: `raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/data/manifest.json`.
- Dataset: `raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/data/restaurants.json`.
- Publish the dataset first and the manifest last.
- The app downloads to a temporary file, checks HTTPS, schema version, record count, SHA-256, and unique keys, then imports in one Room transaction.
- Failed validation leaves the installed version untouched.
- The manifest may require a minimum app version and may declare no update.

Reason: this is a tiny personal dataset and does not justify a backend or release-service layer.

## ADR-003: Update timing

- Manual refresh at any time.
- App-start check only if the last check is at least 24 hours old.
- One unique weekly WorkManager job requiring a network connection, with exponential backoff.
- No duplicate workers and no aggressive polling.

## ADR-004: Map stack

- MapLibre Native renders the map.
- MapTiler Cloud Free supplies the v1 style/tiles for personal, non-commercial use.
- The provider key is stored outside version control for builds. Because APK secrets can be extracted, provider-side restrictions are still required.
- Provider attribution stays visible.
- Public OpenStreetMap tile servers are not used for prefetching or offline packs.
- Full offline basemap support is deferred.

## ADR-005: Coordinates

- Geocode all official RMP addresses once during dataset preparation with the U.S. Census batch geocoder.
- Never geocode the directory at runtime.
- Official RMP coordinates power the primary map marker.
- Store provider, match quality, precision, checked date, and matched address.
- Unmatched or materially mismatched results require manual review.
- A conflicting current address may have its own optional coordinates.

## ADR-006: Location and directions

- Foreground approximate/precise location only; no background location.
- Request permission when Nearby is first used.
- Use a one-shot fused location request and retain no location history.
- Hand transit/walking navigation to Google Maps with a generic `ACTION_VIEW` fallback.
- If official and current addresses conflict, Mike chooses the destination explicitly.

## ADR-007: Open-now calculation

- Timezone is always `America/New_York` for this dataset.
- Hours are normalized into daily intervals.
- Overnight service is split at midnight into two day intervals.
- `24:00` is permitted only as an interval end.
- Closed days use an empty interval list; unknown hours use `null` plus an explicit status.
- Only `verified` or `usable` hours with a non-closed/non-conflicting business status may produce `openNow=true`.

## ADR-008: Images

- Images are optional and never block launch.
- Remote images may be added only with source and attribution/license metadata.
- Missing or failed images use a clean category/brand fallback.
- Images are not bundled for all 241 restaurants in v1.

## Official research references

- Android offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
- Android data layer: https://developer.android.com/topic/architecture/data-layer
- MapLibre Android: https://maplibre.org/maplibre-native/docs/book/platforms/android/index.html
- MapLibre Android examples: https://maplibre.org/maplibre-native/android/examples/
- MapTiler Cloud pricing: https://www.maptiler.com/cloud/pricing/
- Census Geocoder documentation: https://www.census.gov/programs-surveys/geography/technical-documentation/complete-technical-documentation/census-geocoder.html
- Census Geocoder API: https://geocoding.geo.census.gov/geocoder/Geocoding_Services_API.html

