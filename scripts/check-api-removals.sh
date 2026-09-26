#!/usr/bin/env bash
# Fails when a declaration published in a release is missing from the working API dumps.
#
# apiCheck compares against each module's api/, which apiDump rewrites along with any change, so a
# removal dumped in the same commit passes it. This compares against the dumps as they were at a
# release tag instead. A major version bump is allowed to remove things.
#
# Usage: scripts/check-api-removals.sh [tag]
# Without a tag it takes the latest v* tag reachable from HEAD's parent, so a build of the release
# tag itself is checked against the release before it.
set -euo pipefail
# The dumps are found relative to the repository's root.
cd "$(git rev-parse --show-toplevel)"
# One collation for sort and comm on every runner: publish.yml runs this on macOS.
export LC_ALL=C

if [ -n "${1:-}" ]; then
  ref="$1"
else
  ref="$(git describe --tags --abbrev=0 --match 'v*' HEAD^ 2>/dev/null)" || {
    echo "::error::No v* tag is reachable from HEAD^. Fetch tags (actions/checkout needs fetch-depth: 0) or pass one." >&2
    exit 2
  }
fi
# A ref that does not resolve would make every git show below fail quietly and the check pass.
git rev-parse --verify --quiet "$ref^{commit}" > /dev/null || {
  echo "::error::$ref is not a commit in this clone." >&2
  exit 2
}
echo "Comparing the API dumps against $ref."

# Only a vX.Y.Z tag says which major version it was; any other ref is compared without the exemption.
current_major="$(sed -n 's/^VERSION_NAME=\([0-9]*\).*/\1/p' gradle.properties)"
ref_version="${ref#v}"
ref_major="${ref_version%%.*}"
if [[ "$ref_major" =~ ^[0-9]+$ ]] && [ "$current_major" -gt "$ref_major" ]; then
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
  if ! git cat-file -e "$ref:$dump" 2> /dev/null; then
    echo "::notice::$dump did not exist at $ref; it has nothing to compare against."
    return
  fi
  # A dump that is gone lost everything it held.
  removed="$(comm -23 <(git show "$ref:$dump" | "$keys") <(if [ -f "$dump" ]; then "$keys" < "$dump"; fi))"
  if [ -n "$removed" ]; then
    echo "::error::$dump lost $(wc -l <<< "$removed") declaration(s) published in $ref:"
    echo "$removed"
    status=1
  fi
}

# Every module's dumps, at the ref and now: a module added since is compared from its first release on, and one
# removed since is caught along with everything it published.
shopt -s nullglob
now=( */api/jvm/*.api */api/*.klib.api )
dumps="$( { git ls-tree -r --name-only "$ref" | grep -E '^[^/]+/api/(jvm/[^/]+\.api|[^/]+\.klib\.api)$' || true; printf '%s\n' "${now[@]}"; } | sort -u)"
while IFS= read -r dump; do
  case "$dump" in
    *.klib.api) check "$dump" klib_keys ;;
    *) check "$dump" jvm_keys ;;
  esac
done <<< "$dumps"
[ "$status" -eq 0 ] && echo "No declaration published in $ref is missing."
exit "$status"
