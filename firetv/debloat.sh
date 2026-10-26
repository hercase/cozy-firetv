#!/bin/sh
# Disable every package in firetv/disable-list.txt (e.g. after a Fire OS update re-enabled some).
#   TV=192.168.100.29 ./firetv/debloat.sh      the main one (AFTKRT, Fire OS 8)
#   TV=192.168.100.15 ./firetv/debloat.sh      the LG one  (AFTMM,  Fire OS 6)
# Nine of the thirty packages don't exist on Fire OS 6; "Unknown package" there is expected.
# One pm command per shell call: chained pm commands can silently stop after the first.
set -eu
: "${TV:?set TV to the address of the TV, e.g. TV=192.168.100.29 $0}"
case "$TV" in *:*) ;; *) TV="$TV:5555" ;; esac
ADB="${ADB:-adb}"
DIR="$(cd "$(dirname "$0")" && pwd)"

"$ADB" connect "$TV" >/dev/null

grep -v '^#' "$DIR/disable-list.txt" | grep -v '^$' | while read -r pkg; do
  printf '%-50s ' "$pkg"
  # </dev/null or adb shell eats the list this loop is reading from, and only the first line runs.
  "$ADB" -s "$TV" shell "pm disable-user --user 0 $pkg" </dev/null 2>&1 | tr -d '\r' \
    | grep -oE 'new state: .*|protected package|Unknown package: .*' | head -1 || echo '?'
done
