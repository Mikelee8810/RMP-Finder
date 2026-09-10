# RMP Finder — Data Readiness

## Verified Notion snapshot

Audited September 9, 2026 from the `RMP Restaurant Snapshot` data source.

| Measure | Count |
|---|---:|
| Total locations | 241 |
| Unique RMP keys | 241 |
| Missing official addresses | 0 |
| Missing ZIP codes | 0 |
| Missing phone numbers | 1 |
| Missing source hour text | 15 |
| Source hours needing refresh | 11 |
| Source hours marked unknown | 12 |
| Export records with usable structured hours | 176 |
| Export records excluded from open-now calculation | 65 |
| Missing websites | 47 |

| Area | Locations |
|---|---:|
| Brooklyn | 100 |
| Manhattan | 52 |
| Queens | 43 |
| Bronx | 38 |
| Staten Island | 5 |
| Westchester | 3 |

The directory is ready for Browse/Search. Unknown and conflicting enrichment does not block development because the app contract represents those states explicitly.

## Notion-to-export field mapping

The September 9 snapshot is the fixed source input. Official OTDA identity stays separate from current-business enrichment when a reviewed conflict exists.

| Snapshot field | Export field | Mapping rule |
|---|---|---|
| `RMP Key` | `rmpKey` | Copied exactly; must remain unique. |
| `Restaurant` | `officialName`, `currentName`, `aliases` | Official name is retained unless a reviewed OTDA correction exists; reviewed current/rebranded names stay separate. |
| `Address`, `City`, `ZIP`, `Area` | `officialAddress`, `borough`, `zip` | Official OTDA address is preserved. Reviewed OTDA corrections are applied without overwriting current-business facts. |
| reviewed address conflicts | `currentAddress` | Added only when the current business resolves to a different reviewed address. |
| `Phone` | `phone` | Copied; unresolved values remain null. |
| `Website` | `website` | Copied; unresolved values remain null. |
| `Hours` | `hours` | Parsed into seven daily interval arrays in `America/New_York` only when the text is complete enough to normalize safely. |
| `Hours Status`, `Hours Verified`, `Notes` | `hoursStatus`, `businessStatus`, `businessCheckedAt`, `conflictFlags` | Converted to explicit usable/partial/stale/conflicting/unknown and business-state enums; reviewed conflicts remain visible. |
| `RMP Verified`, `OTDA Source` | `rmpVerifiedAt`, `sources[]` | Preserved as RMP eligibility evidence. |
| Census/manual geocode evidence | `coordinates` | Official-address latitude/longitude plus provider, match quality, precision, matched address, and checked date. |
| reviewed enrichment evidence | `sources[]` | Adds source roles and checked dates for coordinates, current-address conflicts, hours, phone, and website. |
| no source equivalent | `menuUrl`, `imageUrl`, `imageAttribution` | Explicit null placeholders until curated later. |

Generation is performed by `python3 tools/build_data.py`. Verification is performed by `python3 tools/verify_data.py`.

## Coordinate quality gate

All 241 official addresses have reviewed coordinates in `data/source/geocode-review-2026-09-09.json`: 209 exact Census matches, 20 reviewed Census matches, and 12 manual/fallback results. There are 0 unmatched records and 0 missing coordinates.

Material ZIP, range, unit-suffix, service-road, malformed-address, and street-name conflicts have explicit review notes. Official RMP addresses remain unchanged when current business information differs. The Kings Plaza false Census match was replaced with a reviewed exact-address fallback coordinate.

## Known high-risk record patterns

- OTDA address differs from the current business address.
- A current source marks the location closed while an official site still publishes hours.
- A business is rebranded at the same location.
- Lobby, delivery, and drive-through hours disagree.
- A phone number resolves to a nearby but different storefront.

These are product states, not cleanup failures. The app must surface them instead of inventing certainty.

## Build gate

Code may begin after all items below are complete:

- [x] Product purpose and MVP locked.
- [x] Official-vs-current truth model locked.
- [x] Offline/update behavior locked.
- [x] Map renderer/provider decision locked.
- [x] Location and directions behavior locked.
- [x] Hours/open-now semantics locked.
- [x] Notion-to-export field mapping completed.
- [x] All official addresses geocoded and quality-checked.
- [x] `restaurants.json` generated and schema-valid.
- [x] `manifest.json` generated with checksum/count/version.
- [x] All 241 records pass the export quality gate.

Final verification checks 241 records, 241 unique keys, first/last key stability, zero restaurant-schema errors, zero manifest-schema errors, zero unmatched coordinates, usable-hours completeness, official/current high-risk truth separation, Kings Plaza coordinate correction, manifest version/URL/policy, and SHA-256 equality with `restaurants.json`.
