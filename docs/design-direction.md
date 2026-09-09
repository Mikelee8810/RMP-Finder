# RMP Finder — Design Direction

## Product target

This is a personal utility for Mike, not a generalized public app. Optimize for fast NYC restaurant decisions on a Pixel:

- Default to current location or Bronx
- Make Nearby the primary discovery mode
- Keep the map prominent because distance matters
- Keep the complete list usable without location access or network access
- Prefer direct actions over explanation
- Keep restaurant photos when available because they make recognition faster

## Core screens

### Browse / List

- RMP Finder identity and short value statement
- Search by restaurant name, official name, address, borough, or ZIP
- Nearby and Open now controls
- Borough filtering
- Restaurant cards with:
  - current name
  - distance
  - RMP verified badge
  - 10% meal discount badge
  - business status
  - address
  - hours or an explicit hours-unknown state
  - optional image
  - conflict warning when needed

### Map

- MapLibre map
- Clustered markers when zoomed out
- Individual markers when zoomed in
- Current-location control
- Bottom sheet for nearby restaurants
- List toggle that remains fully usable offline

### Restaurant details

- Optional restaurant image
- Current business name and address
- RMP verified date
- 10% RMP meal discount
- Open/status and hours freshness
- Directions, call, website, and menu actions only when available
- Current business information section
- Official RMP record section
- Prominent warning for name, address, move, rebrand, closure, or source conflicts
- Transit directions handoff

## Trust rules

RMP eligibility and current business reality are separate facts.

- RMP verified means the location is present in the verified RMP directory.
- Open now requires usable hours and a usable current business status.
- Needs review, conflicting, moved, rebranded, and unknown must remain visible.
- The app must never silently replace the official RMP address with a current address.
- If the map uses a current address instead of the official address, the source and reason must be available on the details screen.

## Personal-use defaults

- Ask for location on first use and default to Nearby.
- Fall back to Bronx/NYC when location is unavailable.
- Prioritize Bronx, Brooklyn, and Manhattan in borough controls.
- Store favorites locally; no account or sync is needed.
- Use the installed maps app for walking/transit directions; no routing backend.
- Do not block a restaurant because its phone, website, hours, or image is missing.

## Design guardrails

- Keep the current clean green/cream visual direction.
- Avoid turning the home screen into a dashboard.
- Use larger readable secondary text on the Pixel.
- Do not show dead Website or Menu actions.
- Do not add public-app onboarding, social features, or generic account flows.
