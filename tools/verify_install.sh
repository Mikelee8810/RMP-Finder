#!/usr/bin/env bash
# Install a release APK on a connected Android device and verify what landed.
#
# Run this on the machine the phone is plugged into, with USB debugging on.
# A sandboxed build environment has no USB path to the device, so this step is
# always run by hand.
#
#   tools/verify_install.sh                      # download the latest release
#   tools/verify_install.sh path/to/RMP-Finder-1.1.0.apk
#
# Requires adb. Downloading a release also requires the gh CLI.

set -euo pipefail

PACKAGE="com.mike.rmpfinder"
APK="${1:-}"

fail() { printf '\n%s\n' "FAILED: $*" >&2; exit 1; }
step() { printf '\n== %s ==\n' "$*"; }

command -v adb >/dev/null || fail "adb is not on PATH. Install Android platform-tools."

step "Device"
adb start-server >/dev/null 2>&1 || true
DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "$DEVICES" ]; then
  adb devices
  fail "No authorized device. Plug the phone in, enable USB debugging, and accept the prompt on the phone."
fi
echo "$DEVICES" | while read -r serial; do
  echo "$serial  $(adb -s "$serial" shell getprop ro.product.model | tr -d '\r') (Android $(adb -s "$serial" shell getprop ro.build.version.release | tr -d '\r'))"
done

if [ -z "$APK" ]; then
  step "Download the latest release"
  command -v gh >/dev/null || fail "gh is not on PATH. Pass an APK path instead."
  WORK="$(mktemp -d)"
  gh release download --repo Mikelee8810/RMP-Finder --pattern '*.apk*' --dir "$WORK"
  APK="$(find "$WORK" -name '*.apk' | head -1)"
  [ -n "$APK" ] || fail "The latest release has no APK asset."
  echo "downloaded $(basename "$APK")"

  step "Checksum"
  SUMS="$(find "$WORK" -name '*.apk.sha256' | head -1)"
  if [ -n "$SUMS" ]; then
    EXPECTED="$(awk '{print $1}' < "$SUMS")"
    ACTUAL="$(shasum -a 256 "$APK" | awk '{print $1}')"
    echo "published: $EXPECTED"
    echo "local:     $ACTUAL"
    [ "$EXPECTED" = "$ACTUAL" ] || fail "The downloaded APK does not match its published checksum."
    echo "checksum matches"
  else
    echo "No .sha256 asset published alongside the APK; skipping the checksum check."
  fi
fi

[ -f "$APK" ] || fail "No such APK: $APK"

step "Installed build before"
BEFORE="$(adb shell dumpsys package "$PACKAGE" | awk -F= '/versionName=/{print $2; exit}' | tr -d '\r')"
echo "${BEFORE:-not installed}"

step "Install"
# -r upgrades in place and keeps app data. It only works when the new APK is
# signed with the same key as the installed one.
if ! adb install -r "$APK"; then
  cat >&2 <<'HINT'

The install failed. If the message mentions a signature or
INSTALL_FAILED_UPDATE_INCOMPATIBLE, this APK was signed with a different key
than the installed build. Uninstalling first will let it install, but that
clears saved favorites and statuses:

    adb uninstall com.mike.rmpfinder

HINT
  fail "adb install did not succeed."
fi

step "Launch"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 \
  || fail "The app did not launch."
sleep 3
adb shell pidof "$PACKAGE" >/dev/null || fail "The app is not running after launch; it may have crashed on start."
echo "running"

step "Installed build after"
adb shell dumpsys package "$PACKAGE" | awk -F= '/versionName=|versionCode=/{print $0}' | head -2 | tr -d '\r'

step "Crashes since launch"
CRASH="$(adb logcat -d -b crash -t 200 | grep -i "$PACKAGE" || true)"
if [ -n "$CRASH" ]; then
  echo "$CRASH"
  fail "The crash log mentions this package."
fi
echo "none"

printf '\nInstalled and launched successfully. Check by hand: the list shows 241 of 241 RMP locations, a detail screen shows its logo or fallback tile, Transit and Walk open Google Maps, and the "Where this comes from" section lists sources.\n'
