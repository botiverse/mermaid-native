#!/usr/bin/env bash
set -euo pipefail

TEST_FILE="mermaid-web/src/wasmJsTest/kotlin/build/raft/mermaid/web/MermaidWebWasmExportsTest.kt"
BACKUP="$(mktemp)"
LOG="$(mktemp)"
cleanup() {
  cp "$BACKUP" "$TEST_FILE"
  rm -f "$BACKUP" "$LOG"
}
trap cleanup EXIT
cp "$TEST_FILE" "$BACKUP"
python3 - "$TEST_FILE" <<'PY'
from pathlib import Path
import sys
path = Path(sys.argv[1])
text = path.read_text()
old = 'val result = renderMermaidResultJson(example.source)'
new = 'val result = error("MUTATION_CONSUMER_CALL_REMOVED")'
if text.count(old) != 1:
    raise SystemExit(f"expected one production consumer call, found {text.count(old)}")
path.write_text(text.replace(old, new))
PY
if ./gradlew :mermaid-web:wasmJsTest --no-daemon >"$LOG" 2>&1; then
  printf '%s\n' 'consumer mutation unexpectedly passed'
  exit 1
fi
grep -q 'exportedJsonRendersEveryPositiveGalleryFixture' "$LOG" || {
  printf '%s\n' 'consumer mutation failed for an unrelated reason'
  cat "$LOG"
  exit 1
}
printf '%s\n' 'consumer mutation RED as expected'
