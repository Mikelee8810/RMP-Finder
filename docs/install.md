# Install RMP Finder on Android

RMP Finder ships as a personal sideload APK from GitHub Releases. Every release
is built and signed by GitHub Actions and published with a SHA-256 checksum.

## Install

1. Open <https://github.com/Mikelee8810/RMP-Finder/releases/latest> on the Android phone.
2. Under **Assets**, download the `RMP-Finder-<version>.apk` file.
3. Open the downloaded APK.
4. If Android asks for permission to install unknown apps, open **Settings**, enable **Allow from this source** for the app that opened the APK, then go back.
5. Tap **Install**.
6. Tap **Open**.

The app opens directly to the bundled 241-location RMP directory. Network access
is not required for the directory, search, filters, favorites, or details.

## Upgrading an installed copy

Android only installs an upgrade when the new APK carries the same signing
certificate as the installed one. Releases are signed with the key 1.0.0 shipped
with, and the release workflow fails before publishing if the signer does not
match, so an upgrade installs straight over the existing app and keeps saved
favorites and statuses.

If Android ever reports `App not installed` or a signature conflict, the build
was signed with a different key. Uninstalling first will install it, but that
clears saved favorites and statuses.

## Verifying the download

Each release also publishes `RMP-Finder-<version>.apk.sha256`. To check a
download before installing it:

```sh
shasum -a 256 RMP-Finder-<version>.apk
```

Compare the result with the value in the `.sha256` asset and with the SHA-256
printed in the release notes. The release workflow already re-downloads the
published APK and fails if it does not match the build output, so a mismatch
here means the local copy was corrupted in transit.

## Verification record

| Version | Build | Physical install |
| --- | --- | --- |
| 1.0.0 | Local workstation build, v2-signed | Verified 2026-09-10 on a Pixel 10 Pro XL running Android 17. Installed, launched, and displayed `241 of 241 RMP locations`. |

The v1 acceptance evidence is recorded in [android-v1-acceptance.md](android-v1-acceptance.md).
Release mechanics and signing are documented in [release.md](release.md).
