#!/bin/sh
# Undo the debloat and give the Amazon launcher back.
#   TV=192.168.100.29 ./firetv/restore.sh              re-enable every package in the list
#   TV=192.168.100.29 ./firetv/restore.sh <package>    re-enable just one
# Cosy Home itself is left installed; the last lines say how to remove it.
set -eu
: "${TV:?set TV to the address of the TV, e.g. TV=192.168.100.29 $0}"
case "$TV" in *:*) ;; *) TV="$TV:5555" ;; esac
ADB="${ADB:-adb}"
DIR="$(cd "$(dirname "$0")" && pwd)"
SVC=com.cosyhome.tv/com.cosyhome.tv.HomeButtonService

"$ADB" connect "$TV" >/dev/null

enable() {
  printf '%-50s ' "$1"
  "$ADB" -s "$TV" shell "pm enable --user 0 $1" </dev/null 2>&1 | tr -d '\r' | tail -1
}

if [ $# -gt 0 ]; then enable "$1"; exit 0; fi

grep -v '^#' "$DIR/disable-list.txt" | grep -v '^$' | while read -r pkg; do enable "$pkg"; done

enable com.amazon.tv.launcher
"$ADB" -s "$TV" shell "cmd package set-home-activity com.amazon.tv.launcher/.ui.HomeActivity_vNext" </dev/null

# Drop Cosy Home's accessibility service, keeping any other the TV had enabled. On Fire OS 6 this
# is a no-op: there the Home redirect is Launcher Manager (com.wolf.minilm), configured in its app.
CUR=$("$ADB" -s "$TV" shell "settings get secure enabled_accessibility_services" </dev/null | tr -d '\r\n')
REST=$(printf '%s' "$CUR" | tr ':' '\n' | grep -v "^$SVC\$" | grep -v '^null$' | paste -sd: -)
if [ -z "$REST" ]; then
  "$ADB" -s "$TV" shell "settings delete secure enabled_accessibility_services" </dev/null
  "$ADB" -s "$TV" shell "settings put secure accessibility_enabled 0" </dev/null
else
  "$ADB" -s "$TV" shell "settings put secure enabled_accessibility_services '$REST'" </dev/null
fi

echo
echo "Done. Optional: remove the launcher with"
echo "  adb -s $TV uninstall com.cosyhome.tv"
