#!/bin/sh
# Regenerates docs/diagrams/<name>.light.svg and <name>.dark.svg from the *.drawio sources
# (draw.io command line, in a container without network access). Transparent background.
# draw.io exports colors as light-dark(light, dark); this script turns each export into two SVGs with fixed colors,
# shown in Markdown with <picture> so that they follow the theme selected on GitHub:
#   <picture>
#     <source media="(prefers-color-scheme: dark)" srcset="docs/diagrams/<name>.dark.svg">
#     <img alt="…" src="docs/diagrams/<name>.light.svg">
#   </picture>
# Run it after editing a .drawio file, then commit the .drawio file and its two .svg files together.
# Usage: sh docs/export-diagrams.sh [png]     (png: also writes a .png preview to $PREVIEW_DIR, /tmp by default)
# Requires Docker and the rlespinasse/drawio-desktop-headless image.
set -eu
D=$(cd "$(dirname "$0")/diagrams" && pwd)
ROOTLESS_SOCKET=/run/user/$(id -u)/docker.sock
if [ -z "${DOCKER_HOST:-}" ] && [ -S "$ROOTLESS_SOCKET" ]; then export DOCKER_HOST="unix://$ROOTLESS_SOCKET"; fi
IMG=rlespinasse/drawio-desktop-headless:latest
# One color: rgb(...), #hex or a name.
C='([a-z]+\([^)]*\)|[^(),]+)'
for f in "$D"/*.drawio; do
  n=$(basename "$f" .drawio)
  docker run --rm --network none --shm-size=1g -e HOME=/tmp -v "$D:/data" -w /data "$IMG" -x -f svg -t -b 10 -o "$n.auto.svg" "$n.drawio" 2>&1 | grep -E -- 'rror' || true
  sed -E "s/light-dark\($C, *$C\)/\1/g; s/color-scheme: light dark/color-scheme: light/" "$D/$n.auto.svg" > "$D/$n.light.svg"
  sed -E "s/light-dark\($C, *$C\)/\2/g; s/color-scheme: light dark/color-scheme: dark/" "$D/$n.auto.svg" > "$D/$n.dark.svg"
  rm "$D/$n.auto.svg"
  if grep -q 'light-dark' "$D/$n.light.svg" "$D/$n.dark.svg"; then echo "ERROR: light-dark() left in $n" >&2; exit 1; fi
  echo "$n.drawio -> $n.light.svg, $n.dark.svg"
  if [ "${1:-}" = png ]; then
    docker run --rm --network none --shm-size=1g -e HOME=/tmp -v "$D:/data" -w /data "$IMG" -x -f png -t -b 10 -s 1.5 -o "$n.preview.png" "$n.drawio" 2>&1 | grep -E -- 'rror' || true
    mv "$D/$n.preview.png" "${PREVIEW_DIR:-/tmp}/" && echo "preview: ${PREVIEW_DIR:-/tmp}/$n.preview.png"
  fi
done
