#!/usr/bin/env bash
# Shallow, sparse fetch of one Mermaid revision (default: the pinned one in
# compatibility/upstreams.lock) containing only what the corpus extractor reads.
# Usage: fetch-upstream.sh [<sha-or-ref>] [<dir>]
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
ref="${1:-}"
dir="${2:-${here}/../../build/render-parity/upstream-mermaid}"
if [ -z "$ref" ]; then
  ref="$(sed -n 's/^mermaid=//p' "${here}/../../compatibility/upstreams.lock")"
fi
rm -rf "$dir"
mkdir -p "$dir"
cd "$dir"
git init -q
git remote add origin https://github.com/mermaid-js/mermaid
git sparse-checkout set --no-cone /cypress/integration/rendering/ /packages/mermaid/package.json /LICENSE
git fetch -q --depth 1 --filter=blob:none origin "$ref"
git -c advice.detachedHead=false checkout -q FETCH_HEAD
echo "mermaid $(git rev-parse HEAD) package $(sed -n 's/^  "version": "\(.*\)",/\1/p' packages/mermaid/package.json)"
