# Building and releasing RMP Finder

RMP Finder releases are built, signed and published by GitHub Actions. CI is the
source of truth for a release build: it is reproducible, it re-runs the data and
logo gates on every build, and it re-downloads the published APK to confirm the
GitHub-hosted copy matches what it built.

## Workflows

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `android-ci.yml` | push, pull request | Dataset gate, logo asset gate, unit tests, debug APK |
| `release-apk.yml` | manual | Signed release APK, GitHub Release, SHA-256, integrity read-back |
| `refresh-logos.yml` | manual | Re-captures restaurant website icons and commits the result |

## Signing

Android will not install an unsigned APK, and it will not install an APK as an
upgrade when the new build is signed with a different key than the installed
build. The signing key therefore has to be stable across releases.

### What 1.0.0 used

The published 1.0.0 APK is v2-signed with the Android **debug** keystore from the
machine that built it:

```
subject      CN = Android Debug, O = Android, C = US
valid from   2026-08-10
SHA-256      97:30:23:F7:B7:91:A9:DA:C0:7E:1B:8C:F1:65:9D:F1:ED:44:1F:41:83:8C:04:02:9D:DB:2C:5D:E4:CE:10:84
```

A debug keystore holds a randomly generated private key created on that machine.
It cannot be regenerated anywhere else. Continuing to sign with it requires
copying that exact file; signing with any other key requires uninstalling 1.0.0
before installing the next build, which clears saved favorites and statuses.

### Required repository secrets

Add these under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `RMP_KEYSTORE_BASE64` | The keystore file, base64 encoded |
| `RMP_KEYSTORE_PASSWORD` | Keystore password |
| `RMP_KEY_ALIAS` | Key alias inside the keystore |
| `RMP_KEY_PASSWORD` | Key password |
| `MAPTILER_KEY` | Optional. Without it the Map tab shows its outage state; everything else still works offline. |

`release-apk.yml` fails immediately with a clear message when
`RMP_KEYSTORE_BASE64` is missing, rather than publishing an APK that cannot be
installed.

### The signer is checked before anything is published

After building, the workflow reads the APK's signing certificate and compares it
to the fingerprint 1.0.0 shipped with. A mismatch fails the run before a release
exists, so a wrong or re-generated keystore cannot quietly produce an APK that
forces an uninstall. To change keys on purpose, set the repository variable
`EXPECTED_SIGNER_SHA256` (Settings → Secrets and variables → Actions →
Variables) to the new certificate's SHA-256.

### Reusing the existing 1.0.0 key

On the machine that built 1.0.0:

```sh
base64 -i ~/.android/debug.keystore | pbcopy
```

Paste that into `RMP_KEYSTORE_BASE64`. The Android debug keystore always uses
these values:

- `RMP_KEYSTORE_PASSWORD`: `android`
- `RMP_KEY_ALIAS`: `androiddebugkey`
- `RMP_KEY_PASSWORD`: `android`

Confirm the fingerprint matches the one above before relying on it:

```sh
keytool -list -v -keystore ~/.android/debug.keystore -storepass android | grep SHA256
```

### Creating a fresh release key instead

```sh
keytool -genkeypair -v \
  -keystore rmp-release.jks \
  -alias rmp-finder \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=RMP Finder, O=Personal, C=US"
base64 -i rmp-release.jks | pbcopy
```

Keep `rmp-release.jks` backed up somewhere durable and outside the repository.
Losing it means every future release needs an uninstall first. A build signed
with this key cannot upgrade an installed 1.0.0, so 1.0.0 must be uninstalled
once before installing the first build that uses it.

## Cutting a release

Make sure the branch is green in **Android CI** first, then use any of these:

**Push a release branch.** Useful where tag pushes are not permitted; the
workflow creates the real tag itself when it publishes.

```sh
git push origin HEAD:release/v1.1.0
```

**Push a tag.** This works from any branch.

```sh
git tag v1.1.0
git push origin v1.1.0
```

**Or use the Actions tab.** Open **Actions → Release APK → Run workflow** and
enter the version name. GitHub only offers manual runs for workflows that are on
the default branch, so this appears once these workflows are on `main`.

The version code is derived from the version name as
`major * 10000 + minor * 100 + patch`, so it always increases with the version
and cannot be entered wrong. `1.1.0` becomes `10100`; the installed 1.0.0 build
used version code `1`.

The workflow refuses to run when a release for that tag already exists, so an
existing release is never overwritten.

The run publishes `RMP-Finder-<version>.apk` and `RMP-Finder-<version>.apk.sha256`,
then downloads the published APK back from GitHub and fails if the checksum does
not match the build output. The run summary records the tag, commit, filename,
SHA-256 and the integrity result.

## What the release APK contains

The release build packages native libraries for `arm64-v8a` only.

MapLibre ships its renderer as a native library per ABI. The 1.0.0 APK carried
all four, which was 49.0 MB of its 70.4 MB: `x86_64` 13.4 MB and `x86` 13.2 MB,
which only ever run on an emulator, plus `armeabi-v7a` 9.5 MB of 32-bit ARM the
target Pixel does not use.

Measured on the first release build after the change, the APK is **29.1 MB**,
down from 70.4 MB, while also carrying 1.49 MB of restaurant logos that 1.0.0
did not have:

| component | size | share |
| --- | --- | --- |
| `*.dex` | 13.87 MB | 47.6% |
| `lib/arm64-v8a` | 12.84 MB | 44.1% |
| `assets/restaurant-logos/` | 1.49 MB | 5.1% |
| `resources.arsc` | 0.64 MB | 2.2% |
| everything else | ~0.3 MB | 1.0% |

Debug builds deliberately keep every ABI so emulator-based instrumented tests in
`app/src/androidTest/` still run. Both workflows measure the built APK with
`tools/apk_report.py` and fail if it packages anything other than `arm64-v8a`,
so the size claim is measured rather than assumed.

R8 code shrinking is **not** enabled. It would cut into the 20.6 MB of dex, but
the app uses Room and MapLibre's JNI callbacks, both of which can need keep
rules, and a missing rule usually shows up as a crash at runtime rather than a
build failure. Nothing in this project's build environment can launch a release
APK, so enabling it would mean shipping it unverified. Turn it on alongside a
real install check on the device.

## Building locally

A local release build needs the Android SDK plus `dl.google.com` for the Android
Gradle Plugin and the AndroidX artifacts. In a restricted network environment
those hosts are unavailable and no local Android build is possible; use the
workflows above. Where the network does allow it:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
python3 tools/verify_data.py
python3 tools/check_logo_assets.py
```

Put `MAPTILER_KEY` and any signing values in `local.properties`; that file is
ignored by git.
