# Claude handoff — RMP Finder — 2026-09-11

Continues `claude-handoff-2026-09-10.md`. Do not restart settled research and do
not replace the 241-record official RMP membership baseline.

## Where this left off

Branch: `claude/sharp-gates-k8pydt`.

The app code is finished for this round. The remaining work to a published
1.1.0 release is not code: it needs repository secrets that only Mike can add,
and a physical install check on his Pixel.

## What changed this session

**Stale UI copy was already done.** The cleanup the previous handoff asked for
had landed in `907c2f1`. Verified by searching for every retired string rather
than assuming; nothing was left to remove.

**Eight unusable logo assets removed.** Three were fully transparent 16x16 files
that rendered as empty brand tiles. Five more were 16-32px favicons that blur
badly at the 52dp and 76dp sizes the mark is drawn at. The built-in initial and
category fallback reads better than any of them. 35 assets remain.

**The curator now has a quality gate.** `tools/build_logos.py` rejects an icon
below 48x48 or with fewer than three visible colors, and records the reason in
the discovery evidence instead of silently claiming a capture. It also tries
first-party brand icon URLs before generic homepage discovery, rasterizes an SVG
mark when one is offered, and keeps an earlier run's icon when a site is
unreachable. `tools/check_logo_assets.py` enforces the gate and the
evidence/asset reconciliation in CI.

**Logo decoding was fixed.** The brand mark decoded its PNG inside the row
composable with only a `remember` cache, so scrolling the 241-row list re-decoded
the same handful of logos repeatedly on the composition path, holding 512px
sources at full resolution for a mark drawn at 52dp. Decoding now happens once
per asset behind a process-level cache and downsamples to roughly the drawn
size: about 9 MB worst case instead of about 37 MB.

**The release APK is arm64 only.** The published 1.0.0 APK was 70.4 MB, of
which 49.0 MB was MapLibre's renderer duplicated across four ABIs: `x86_64`
13.4 MB and `x86` 13.2 MB that only ever run on an emulator, and `armeabi-v7a`
9.5 MB of 32-bit ARM the Pixel does not use. The release now filters to
`arm64-v8a`, removing about 36 MB with no behavior change on the device. Debug
builds keep every ABI so emulator instrumented tests still run.
`tools/apk_report.py` measures the built APK in both workflows and fails if it
packages anything else, so the size is measured rather than asserted.

**The project can now build itself.** There was no Gradle wrapper and no CI; a
release depended entirely on one workstation. Added the wrapper (Gradle 9.6.0,
which is what AGP 9.4.0 requires) and three workflows described in
`docs/release.md`.

## Environment constraints found

These are properties of the sandbox, not of the project. They will bite any
future session that tries to build locally.

- `dl.google.com` is blocked by egress policy, and `maven.google.com` only
  redirects there. There is no Android SDK and no AndroidX or AGP artifacts, so
  **no local Android build is possible**. GitHub Actions is the build path.
- The general web is blocked too. Restaurant and brand sites are unreachable, so
  **logo capture cannot run locally**. `refresh-logos.yml` runs it on Actions.
- `services.gradle.org` is reachable but redirects distribution downloads to
  `downloads.gradle.org`, which is blocked. The wrapper jar is therefore the
  8.14.3 bootstrapper pointing at the 9.6.0 distribution, which is supported.
- `workflow_dispatch` only works for workflows on the default branch, so the
  release workflow also accepts a tag push, which works from any branch.

## Blocking the 1.1.0 release

Mike chose to reuse the signing key 1.0.0 shipped with, so the upgrade installs
over the existing app and keeps saved favorites and statuses.

Add these under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `RMP_KEYSTORE_BASE64` | `base64 -i ~/.android/debug.keystore` from the Mac that built 1.0.0 |
| `RMP_KEYSTORE_PASSWORD` | `android` |
| `RMP_KEY_ALIAS` | `androiddebugkey` |
| `RMP_KEY_PASSWORD` | `android` |
| `MAPTILER_KEY` | The same map key 1.0.0 was built with |

Then push a tag:

```sh
git tag v1.1.0 && git push origin v1.1.0
```

The workflow verifies the built APK's signing certificate against the
fingerprint 1.0.0 shipped with and fails before publishing anything if it does
not match, so a wrong keystore cannot produce an APK that forces an uninstall.

## Still open

- **Chain logo coverage.** 36 assets cover 72 of 241 restaurants. The rest fall
  back to the initial and category tile. A refresh run on 2026-09-11 could not
  capture the big chains, and the recorded reasons are specific rather than
  general flakiness:

  | domain | restaurants | failure |
  | --- | --- | --- |
  | `mcdonalds.com` | 48 | `ReadTimeout` |
  | `popeyes.com` | 28 | response was not a decodable image |
  | `kfc.com` | 4 | `HTTPError` |
  | `checkers.com` | 1 | `HTTPError` |
  | `locations.goldenkrust.com` | 1 | response was not a decodable image |

  A further 47 restaurants have no website at all, so there is nothing to fetch
  for them. The timeout has since been raised to 20 seconds, which may be enough
  for McDonald's alone; the others look like bot protection or wrong candidate
  URLs and would need the brand marks supplied by hand.

  Name-based brand aliasing was measured and rejected: it would have covered
  exactly one more restaurant.

  Coverage reads lower than the 81 recorded before this session. That is
  deliberate: nine assets were removed because they were blank tiles, 16px
  favicons upscaled to 80dp, or, in one case, the WordPress default logo. Those
  restaurants now show the built-in fallback, which is correct rather than
  broken or misleading.
- **Physical install.** 1.1.0 has not been installed on the Pixel. That is the
  only device-dependent step and it was deliberately left until the device is
  back.
- **R8.** Code shrinking is off. It would cut into the 20.6 MB of dex, but Room
  and MapLibre's JNI callbacks can need keep rules and a missing rule usually
  surfaces as a runtime crash rather than a build failure. Nothing in this build
  environment can launch a release APK, so turning it on belongs with a real
  install check on the device.

## Invariants to preserve

- Exactly 241 official RMP locations from `data/source/notion-rmp-snapshot-2026-09-09.json`.
- Area counts: Bronx 38, Brooklyn 100, Manhattan 52, Queens 43, Staten Island 5, Westchester 3.
- Official OTDA facts stay separate from current-business facts.
- A broken or empty OTDA live county response is not evidence that official locations disappeared.
- The app stays offline-first.
- The known high-risk business-truth mismatches listed in the 2026-09-10 handoff
  stay represented as source discrepancies, never silently overwritten.

## Notification incident

Unchanged. The Pixel notification issue appears to have stopped after AirSync was
uninstalled. AirSync remains the strongest suspect, but that is not a confirmed
root cause. Do not change phone notification settings.
