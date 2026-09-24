#!/usr/bin/env bash
# Fails when a declaration published in a release is missing from the working API dumps.
#
# apiCheck compares against colorpicker/api/, which apiDump rewrites along with any change, so a
# removal dumped in the same commit passes it. This compares against the dumps as they were at a
# release tag instead. A major version bump is allowed to remove things.
#
# Usage: scripts/check-api-removals.sh [tag]
# Without a tag it takes the latest v* tag reachable from HEAD's parent, so a build of the release
# tag itself is checked against the release before it.
set -euo pipefail

if [ -n "${1:-}" ]; then
  ref="$1"
else
  ref="$(git describe --tags --abbrev=0 --match 'v*' HEAD^ 2>/dev/null)" || {
    echo "::error::No v* tag is reachable from HEAD^. Fetch tags (actions/checkout needs fetch-depth: 0) or pass one." >&2
    exit 2
  }
fi
echo "Comparing the API dumps against $ref."

current_major="$(sed -n 's/^VERSION_NAME=\([0-9]*\).*/\1/p' gradle.properties)"
ref_version="${ref#v}"
if [ "$current_major" -gt "${ref_version%%.*}" ]; then
  echo "VERSION_NAME is a major bump over $ref; removals are allowed."
  exit 0
fi

# JVM dump members carry no class name, so each is keyed by the class it sits in, and classes by
# name alone: a header that gains a supertype is not a removal. "synthetic" is dropped because
# @Deprecated(level = HIDDEN) adds it to a declaration that binaries can still link against.
jvm_keys() {
  awk '
    /^[^\t].*\{$/ { for (i = 1; i <= NF; i++) if ($i ~ /\//) { cls = $i; break }; print "class " cls; next }
    /^\t/ { line = $0; sub(/ synthetic /, " ", line); print cls " ::" line }
  ' | grep -v -e 'ComposableSingletons' -e 'getLambda\$' | sort -u
}

# Every klib dump line carries its full signature after " // ", which is already a unique key.
klib_keys() {
  grep -E '^.+ // ' | sed 's#^.* // ##' | grep -v 'ComposableSingletons' | sort -u
}

status=0
check() {
  local dump="$1" keys="$2" removed
  removed="$(comm -23 <(git show "$ref:$dump" | "$keys") <("$keys" < "$dump"))"
  if [ -n "$removed" ]; then
    echo "::error::$dump lost $(wc -l <<< "$removed") declaration(s) published in $ref:"
    echo "$removed"
    status=1
  fi
}

check colorpicker/api/jvm/colorpicker.api jvm_keys
check colorpicker/api/colorpicker.klib.api klib_keys
[ "$status" -eq 0 ] && echo "No declaration published in $ref is missing."
exit "$status"
