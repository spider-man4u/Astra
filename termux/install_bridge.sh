#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
ASTRA_HOME="${ASTRA_HOME:-$HOME/astra}"
TARGET="$ASTRA_HOME/bridge_server.py"

if [ ! -f "$ASTRA_HOME/astra.py" ]; then
  echo "ASTRA backend not found at $ASTRA_HOME/astra.py"
  echo "Install/upgrade ASTRA v2 first. This installer will not create or replace your backend."
  exit 1
fi

mkdir -p "$ASTRA_HOME"
if [ -e "$TARGET" ]; then
  BACKUP="$TARGET.backup.$(date +%Y%m%d-%H%M%S)"
  cp -p "$TARGET" "$BACKUP"
  echo "Existing bridge backed up to: $BACKUP"
fi

install -m 700 "$SOURCE_DIR/bridge_server.py" "$TARGET"
python "$TARGET" --help >/dev/null
echo "Bridge installed at: $TARGET"
echo
echo "Start it with:"
echo "  python ~/astra/bridge_server.py"
echo
echo "Show the access token later with:"
echo "  python ~/astra/bridge_server.py --show-token"
