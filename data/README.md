# Static restaurant data

The Android app consumes a validated export generated from the Notion RMP directory.

## Planned files

- restaurants.json — validated restaurant records
- manifest.json — dataset version, record count, schema version, generation date, and SHA-256
- restaurant.schema.json — validation contract

## Required export behavior

The export must:

1. Reject duplicate rmpKey values.
2. Reject missing official addresses, ZIPs, or RMP verification dates.
3. Require coordinates before the map is considered complete.
4. Preserve official and current addresses separately.
5. Preserve unresolved fields as explicit null, unknown, or conflicting values.
6. Include source and checked-date information for current business data.
7. Produce deterministic output so changes are easy to review.

## Global program policy

The current RMP meal discount is a dataset/app policy value, not a duplicated fact that needs to be repeated in every row:

 rmpDiscountPercent: 10
