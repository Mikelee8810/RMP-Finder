# Android v1 acceptance

Verified on 2026-09-10 against `main` on a Pixel 10 Pro XL running Android 17.

All eight acceptance criteria in `docs/product-requirements.md` have direct PASS evidence.

| # | Acceptance criterion | Result | Direct evidence |
|---|---|---|---|
| 1 | First launch displays the bundled directory with airplane mode enabled. | PASS | `OfflineFirstLaunchAcceptanceTest#freshFirstLaunchAndAllBrowseFiltersWorkInAirplaneMode` passed on the physical Pixel. Before the run, `com.mike.rmpfinder` packages were absent, `airplane_mode_on=1`, Wi-Fi was disabled, and a ping to `1.1.1.1` had 100% packet loss. The test then found `241 of 241 RMP locations`. |
| 2 | Search and every filter work without network access. | PASS | The same airplane-mode test passed offline search plus Bronx, Open now, Needs attention, and Favorites flows, including detail navigation. `DatasetAcceptanceTest#searchCoversOfficialAndCurrentAddressOffline` and `#boroughOpenFavoritesAndAttentionFiltersWorkOffline` also pass. |
| 3 | Denied location permission does not block Browse or Map browsing. | PASS | `AppAcceptanceTest#deniedLocationStillShowsBundledBrowseAndMapList` passed on the physical Pixel. |
| 4 | A tile-provider outage does not block list/detail access. | PASS | `AppAcceptanceTest#configuredMapLoadFailureStillLeavesListAndDetailUsable` passed with `MAPTILER_KEY=dummy` and `MAP_STYLE_URL_OVERRIDE=http://127.0.0.1:9/style.json`, forcing the map style request to fail. |
| 5 | Invalid JSON, schema failure, wrong record count, or checksum mismatch cannot replace installed data. | PASS | `DatasetAcceptanceTest#schemaFailureIsRejected`, `#recordCountMismatchIsRejected`, and `#checksumMismatchIsRejectedBeforeImport` pass. `RepositoryAcceptanceTest#invalidDownloadedDatasetCannotReplaceInstalledDirectory` also passed on the physical Pixel. |
| 6 | Conflict records visibly separate the official RMP location from current business information. | PASS | `DatasetAcceptanceTest#officialAndCurrentAddressesStaySeparateForKnownConflict` and `AppAcceptanceTest#knownConflictShowsOfficialAndCurrentAddressesSeparately` pass. |
| 7 | No unavailable phone, website, menu, or image action is rendered. | PASS | `AppAcceptanceTest#missingPhoneDoesNotRenderCallAction`, `#missingWebsiteAndMenuDoNotRenderActions`, and `#missingImageDoesNotRenderImageAction` all pass. |
| 8 | No unknown/conflicting-hours record is labeled open. | PASS | `DatasetAcceptanceTest#unsafeHoursNeverProduceOpenNow` passes. |

## Verification runs

- `:app:testDebugUnitTest :app:assembleDebug` — BUILD SUCCESSFUL.
- Fresh-install airplane-mode acceptance — BUILD SUCCESSFUL on Pixel 10 Pro XL / Android 17.
- Forced map-provider failure acceptance — BUILD SUCCESSFUL on Pixel 10 Pro XL / Android 17.
- `AppAcceptanceTest` under the forced map-provider failure configuration — 6 tests, 0 failures.
- `RepositoryAcceptanceTest` — BUILD SUCCESSFUL on Pixel 10 Pro XL / Android 17.
- `DatasetAcceptanceTest` — 10 tests, 0 failures.

## Data gate carried into Android v1

The bundled runtime data is the already-approved 241-record export described in `docs/data-readiness.md`. `DatasetAcceptanceTest#bundledDatasetPassesRuntimeGate` verifies the bundled manifest/data pair and confirms all 241 records pass the runtime gate.

Android v1 therefore satisfies the full acceptance gate defined in `docs/product-requirements.md`.
