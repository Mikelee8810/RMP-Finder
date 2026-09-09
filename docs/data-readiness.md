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
| Missing usable hours | 15 |
| Hours needing refresh | 11 |
| Hours marked unknown | 12 |
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

## Required transformation before the first app build

Notion currently stores restaurant name, address, area, city, ZIP, phone, website, free-text hours, hours status/date, notes, OTDA source, RMP key, and RMP verification date.

The export step must derive or add:

- Structured official/current names and aliases.
- Structured official/current addresses.
- Business-status enum.
- Normalized weekly hours and timezone.
- Conflict flags parsed from reviewed notes.
- Official-address coordinates and geocode metadata.
- Optional current-address coordinates for address conflicts.
- Source roles and checked dates.
- Optional menu/image metadata.

## Coordinate quality gate

The map is not build-ready until all 241 official addresses have one of:

- an accepted coordinate result with recorded match quality; or
- an explicit manual-review result.

Every automated geocode whose matched ZIP, street number, or normalized street materially differs from the RMP record must be reviewed. Official RMP addresses remain unchanged even when current business information differs.

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
- [ ] Notion-to-export field mapping completed.
- [ ] All official addresses geocoded and quality-checked.
- [ ] `restaurants.json` generated and schema-valid.
- [ ] `manifest.json` generated with checksum/count/version.
- [ ] All 241 records pass the export quality gate.

