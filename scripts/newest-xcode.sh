#!/usr/bin/env bash
# Prints the path of the newest released Xcode in /Applications, or in the directory given.
#
# An app's version is the CFBundleShortVersionString in its Info.plist, compared number by number, so 26.10 is newer
# than 26.9 whatever the app is called. A beta or a release candidate is skipped: the runner images name one
# Xcode_26.1_beta_3.app or Xcode_26.3_Release_Candidate.app, and a beta's LicenseInfo.plist says Beta too. Symlinks
# are skipped, so the release-looking names the images give a beta (Xcode_26.1.app) are not taken for a release; each
# release is listed under its own name.
#
# Usage: scripts/newest-xcode.sh [dir]
set -euo pipefail

dir="${1:-/Applications}"

plist_value() {
  /usr/libexec/PlistBuddy -c "Print :$1" "$2" 2> /dev/null
}

lowercase() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

# One line per released Xcode: its major, minor and patch, then its path, tab-separated. The loop is a function, not
# written inside $(...): macOS's bash 3.2 misparses a case pattern's ) there.
releases() {
  local app version
  for app in "$dir"/Xcode*.app; do
    if [ ! -d "$app" ] || [ -L "$app" ]; then
      continue
    fi
    case "$(lowercase "$(basename "$app")")" in
      *beta* | *release_candidate*) continue ;;
    esac
    case "$(lowercase "$(plist_value licenseType "$app/Contents/Resources/LicenseInfo.plist" || true)")" in
      *beta*) continue ;;
    esac
    version="$(plist_value CFBundleShortVersionString "$app/Contents/Info.plist" || true)"
    if [[ "$version" =~ ^([0-9]+)(\.([0-9]+))?(\.([0-9]+))?$ ]]; then
      printf '%s\t%s\t%s\t%s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[3]:-0}" "${BASH_REMATCH[5]:-0}" "$app"
    fi
  done
}

tab="$(printf '\t')"
newest="$(releases | sort -t "$tab" -k1,1n -k2,2n -k3,3n | tail -n 1 | cut -f 4)"
if [ -z "$newest" ]; then
  echo "::error::No released Xcode with a version in its Info.plist is in $dir." >&2
  exit 1
fi
echo "$newest"
