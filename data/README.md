# Static restaurant data

The Android app consumes a validated export generated from the Notion RMP directory.

## Published pre-build files

- `restaurants.json` — validated 241-record static dataset
- `manifest.json` — dataset version, record count, schema version, generation date, SHA-256, update URL, and program policy
- `restaurant.schema.json` — restaurant validation contract
- `manifest.schema.json` — update-manifest validation contract
- `source/notion-rmp-snapshot-2026-09-09.json` — recovered 241-row source snapshot
- `source/census-geocodes-2026-09-09.json` — raw Census geocoding evidence
- `source/geocode-review-2026-09-09.json` — accepted/reviewed coordinate evidence for all 241 records

Rebuild with `python3 tools/build_data.py` and verify with `python3 tools/verify_data.py` from the repository root.

## Required export behavior

The export must:

1. Reject duplicate rmpKey values.
2. Reject missing official addresses, ZIPs, or RMP verification dates.
3. Require coordinates before the map is considered complete.
4. Preserve official and current addresses separately.
5. Preserve unresolved fields as explicit null, unknown, or conflicting values.
6. Include source and checked-date information for current business data.
7. Produce deterministic `restaurants.json` output so changes are easy to review.
8. Normalize free-text hours into `America/New_York` daily intervals.
9. Record geocode provider, match quality, precision, checked date, and matched address.
10. Publish `restaurants.json` before publishing the manifest that announces it.

## Global program policy

The current RMP meal discount is a dataset/app policy value, not a duplicated fact that needs to be repeated in every row:

`rmpDiscountPercent: 10`
