# Claude handoff — RMP Finder — 2026-09-10

Continue the existing RMP Finder work from the repository state you receive. Do not restart settled research or replace the 241-record official RMP membership baseline.

## Goal

Finish the Android app until it is genuinely install-ready, while keeping every solid checkpoint pushed to GitHub. The final completion state requires a built release APK, GitHub-hosted durable artifacts, matching SHA-256 verification, and physical install/launch verification when Mike's Pixel is available again.

## Current repository state

- Workspace: `/Users/michael/Documents/ChatGPT/RMP Finder`
- Branch: `main`
- Last pushed checkpoint before this handoff: `f1d2dd754f8f7729fe53e71f86ed3fc6db3f5db3`
- At handoff time `HEAD == origin/main` before the pending local batch below is checkpointed.
- The repo README already records the pre-build data gate and Android v1 acceptance gate as complete.

## Data invariants

- Preserve exactly 241 official RMP locations from `data/source/notion-rmp-snapshot-2026-09-09.json`.
- Area counts: Bronx 38, Brooklyn 100, Manhattan 52, Queens 43, Staten Island 5, Westchester 3.
- Preserve official OTDA facts separately from current-business facts.
- Do not treat broken/empty OTDA live county responses as evidence that official locations disappeared.
- Keep the app offline-first.

## Current pending local batch

Modified:

- `app/src/main/java/com/mike/rmpfinder/MainViewModel.kt`
- `app/src/main/java/com/mike/rmpfinder/data/OpenNow.kt`
- `app/src/main/java/com/mike/rmpfinder/data/RmpRepository.kt`
- `app/src/main/java/com/mike/rmpfinder/ui/RmpFinderUi.kt`

Untracked:

- `app/src/main/assets/restaurant-logos/` (45 PNG domain assets at last count)
- `data/source/website-logo-discovery-2026-09-10.json`
- `tools/build_logos.py`

The copy cleanup already changes vague states to specific text such as `Moved`, `Status varies by source`, `Hours partially confirmed`, `Hours not recently confirmed`, `Hours vary by source`, and `Hours unavailable`.

`RmpFinderUi.kt` has restaurant-logo cards/detail display with a local asset loader and fallback initials/category. It still contains obsolete compatibility mappings for the old strings `Moved — check details`, `Status conflict — check details`, and `Hours need review`; remove/simplify those. Also replace any remaining `Open now stays unavailable until these hours are confirmed.` and `Last update issue:` copy with clearer finished-product wording.

## Logo direction

Mike wants real restaurant logos when they are available, with a clean fallback for restaurants that do not have a usable logo. Logos must remain bundled offline rather than loaded from the network at runtime.

Current known state:

- 45 domain PNG assets generated.
- Earlier coverage estimate: about 83/241 restaurants with real marks, 158 fallback.
- Major chain misses included McDonald's, Popeyes, and KFC.
- Official McDonald's raster candidate previously found: `https://www.mcdonalds.com/content/dam/sites/usa/nfl/icons/arches-logo_108x108.jpg`.
- Popeyes/KFC official branding was found as SVG; rasterize at build time if needed instead of adding runtime network dependencies.
- Review/remove generic social captures such as `facebook.com.png` or `instagram.com.png` if they are platform logos rather than restaurant branding.

## Immediate work

1. Inspect the pending diff once and finish the stale UI-copy cleanup.
2. Improve major-chain logo coverage and remove junk logo assets.
3. Run `git diff --check`, unit tests, and an Android debug build; repair any failures.
4. Check logo/fallback rendering using emulator or other available local Android tooling if feasible.
5. Commit and push each sound checkpoint to `main`; verify local `HEAD == origin/main` after pushes.
6. Continue through release APK generation. Use a new release/tag rather than overwriting `v1.0.0`.
7. Upload the APK and SHA-256 to GitHub, download/read back the GitHub-hosted copy, and verify its SHA-256 matches.
8. Mike currently has the Pixel with him. Do not depend on or alter the Pixel until it is connected again. Physical install/launch verification is the only device-dependent step that may remain then.

## Notification incident

The Pixel notification issue appears to have stopped after Mike uninstalled AirSync. Treat AirSync as the strongest current culprit, but do not claim a definitive root cause without new evidence. Do not touch phone notification settings while the Pixel is away.

## Known high-risk business-truth mismatches

Keep these represented as source discrepancies rather than silently overwriting official RMP identity:

- Dunkin: 2370 Grand Concourse Road vs current 2366 Grand Concourse.
- Burger King/Popeyes: 60 Metropolitan Oval.
- Numero Uno Sabor Latino: temporarily closed.
- McDonald's: 204-11 Hillside Avenue borough conflict.
- McDonald's: 1275 Fulton Street and 3540 Nostrand Avenue ZIP conflicts.
- Fresh Pond Burger King/Popeyes: major name/address/ZIP/phone conflict.
- Carnation: 2310 vs current 2322 86th St.
- Roseli Chinese vs Lady Chow Kitchen.

## Completion standard

Do not report “ready to install” merely because the project compiles. Completion means the release APK and its source/data are durably on GitHub, the GitHub copy is integrity-checked, all acceptance/data invariants still pass, and the physical Pixel install/launch is verified once the device is available.
