#!/usr/bin/env bash
# PostToolUse hook: lint Clojure files with clj-kondo after Write/Edit.
# Blocks on errors, passes through on warnings.
# Reads tool input JSON from stdin.
command -v jq >/dev/null || { echo "hook-clj-kondo: jq not found — linting disabled" >&2; exit 0; }

FILE=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null || true)

# Skip non-Clojure files
if [ -z "$FILE" ]; then exit 0; fi
echo "$FILE" | grep -qE '\.(clj[sc]?)$' || exit 0

# Skip if file doesn't exist
if [ ! -f "$FILE" ]; then exit 0; fi

output=$(clj-kondo --lint "$FILE" 2>&1 || true)

# Count actual error lines (": error:" with line number prefix), not the summary
errors=$(echo "$output" | grep -cE ':[0-9]+:[0-9]+: error:' 2>/dev/null || true)
errors="${errors##*$'\n'}"  # take last line only
errors="${errors:-0}"

if [ "$errors" -gt 0 ] 2>/dev/null; then
  echo "{\"decision\":\"block\",\"reason\":\"clj-kondo: ${errors} error(s) in ${FILE}\"}"
  echo "$output" >&2
fi
