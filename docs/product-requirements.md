# RMP Finder — Product Requirements

## Product

RMP Finder is Mike's private Android utility for finding New York Restaurant Meals Program locations. It is optimized for a Pixel phone, Bronx-based travel, public transit, low operating cost, and unreliable connectivity.

## Success criteria

The app succeeds when Mike can quickly answer:

1. Which RMP restaurants are near me or near an address?
2. Is this exact address still listed by OTDA for RMP?
3. Is the current business likely open, and how fresh is that information?
4. How do I get there by transit or walking?
5. Is there a conflict that could make the trip risky?

## MVP scope

### Browse

- Search current name, official name, aliases, address, ZIP, and borough.
- Sort by distance when a location or chosen map point exists; otherwise use Bronx-first then name.
- Filter by borough, open now, favorites, and attention-needed status.
- Show distance, address, current hours state, RMP verification, the 10% meal discount, and conflict warnings.
- Keep search, filters, and favorites fully usable offline.

### Map

- MapLibre map with marker clustering and a nearby-results bottom sheet.
- Marker location represents the official RMP address.
- Current-location button uses a one-time foreground location only.
- The list remains available if permission is denied or map tiles fail.
- Basemap tiles are online-first in v1; restaurant markers and data are local.

### Restaurant details

- Current name and official RMP name.
- Official RMP address and current business address kept separate.
- RMP verification date and 10% meal discount.
- Current business status, weekly hours, freshness date, and source warning.
- Call, website, menu, favorite, and transit/walking directions when available.
- If addresses conflict, directions must clearly offer the official RMP address and current business address rather than silently choosing one.

### Updates

- Show installed dataset version, generated date, and last successful check.
- Manual refresh.
- At most one automatic app-start check per day.
- Weekly opportunistic WorkManager check while connected.
- Preserve the last known-good dataset after every failed update.

## Truth rules

- OTDA controls whether an exact location is listed for RMP.
- Business sources enrich status, hours, phone, website, current name, and current address.
- Business enrichment never silently overwrites the official RMP record.
- `Open now` is true only when business status permits it and verified/usable hours cover the current time.
- Unknown, stale, partial, or conflicting hours are excluded from `Open now` and shown plainly.
- A disappeared OTDA record is flagged for review before removal from Mike's installed directory.

## Personal defaults

- Ask for foreground location when Nearby is first used.
- Bronx is the no-location fallback.
- Borough ordering: Bronx, Brooklyn, Manhattan, Queens, Staten Island, Westchester.
- Directions prioritize transit, then walking.
- Favorites and settings are device-local; no account or sync.

## Explicitly out of v1

- Accounts, backend, admin dashboard, submissions, social features, AI features.
- Runtime geocoding or embedded route calculation.
- Full offline basemap packages.
- Push alerts and menu scraping.
- Coupons/deals and final-price calculation; these remain a phase-two layer.

## Acceptance criteria

- First launch displays the bundled directory with airplane mode enabled.
- Search and every filter work without network access.
- Denied location permission does not block Browse or Map browsing.
- A tile-provider outage does not block list/detail access.
- Invalid JSON, schema failure, wrong record count, or checksum mismatch cannot replace installed data.
- Conflict records visibly separate the official RMP location from current business information.
- No unavailable phone, website, menu, or image action is rendered.
- No unknown/conflicting-hours record is labeled open.

