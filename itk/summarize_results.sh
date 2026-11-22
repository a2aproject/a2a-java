#!/usr/bin/env bash
# Summarize JSON reports produced by the ITK ACTS test run.
#
# By default this reads itk/acts-report-*.json. Pass one or more report paths
# to summarize a particular run instead.
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

usage() {
  cat <<'EOF'
Usage: ./itk/summarize_results.sh [REPORT.json ...]

Summarize ACTS reports produced by ./itk/run_itk_local.sh --acts.

With no arguments, reads itk/acts-report-*.json. The command exits with 1
when a report contains failed or errored tests, so it can be used in CI.

Examples:
  ./itk/summarize_results.sh
  ./itk/summarize_results.sh itk/acts-report-a2a-java-jsonrpc-*.json
EOF
}

if [[ ${1:-} == '-h' || ${1:-} == '--help' ]]; then
  usage
  exit 0
fi

if ! command -v jq >/dev/null; then
  echo 'jq is required to summarize ITK results.' >&2
  exit 2
fi

if (($#)); then
  reports=("$@")
else
  shopt -s nullglob
  reports=("$script_dir"/acts-report-*.json)
  shopt -u nullglob
fi

if ((${#reports[@]} == 0)); then
  echo 'No ACTS result reports found. Run ./itk/run_itk_local.sh --acts first.' >&2
  exit 2
fi

format_duration() {
  local milliseconds=$1
  if ((milliseconds < 1000)); then
    printf '%d ms' "$milliseconds"
  else
    awk -v milliseconds="$milliseconds" 'BEGIN { printf "%.1f s", milliseconds / 1000 }'
  fi
}

total=0
passed=0
failed=0
skipped=0
errors=0
duration_ms=0

for report in "${reports[@]}"; do
  if [[ ! -f $report ]]; then
    echo "Report does not exist: $report" >&2
    exit 2
  fi
  if ! jq -e '.summary and .suites and .transport' "$report" >/dev/null; then
    echo "Not an ACTS result report: $report" >&2
    exit 2
  fi

  transport=$(jq -r '.transport' "$report")
  generated_at=$(jq -r '.generated_at // "unknown"' "$report")
  sdk=$(jq -r '.sdk.name // "unknown SDK"' "$report")
  read -r report_total report_passed report_failed report_skipped report_errors report_duration <<EOF
$(jq -r '[.summary.total, .summary.passed, .summary.failed, .summary.skipped, .summary.errors, .summary.duration_ms] | @tsv' "$report")
EOF

  printf '\n%s (%s, %s)\n' "$sdk" "$transport" "$generated_at"
  printf '  Total: %s  Passed: %s  Failed: %s  Errors: %s  Skipped: %s  Duration: %s\n' \
    "$report_total" "$report_passed" "$report_failed" "$report_errors" "$report_skipped" "$(format_duration "$report_duration")"
  printf '  By conformance level:\n'
  jq -r '.summary.by_level // {} | to_entries[] | "    \(.key): \(.value.passed)/\(.value.total) passed; \(.value.failed) failed; \(.value.errors) errors; \(.value.skipped) skipped"' "$report"

  failures=$(jq -r '
    .suites[] as $suite
    | $suite.tests[]
    | select(.result == "fail" or .result == "error")
    | [
        .result,
        .level,
        $suite.name,
        .id,
        .name,
        (.failure.step_id // ""),
        (.failure.assertion_path // ""),
        (.failure.message // ""),
        (.failure.expected // ""),
        (.failure.actual // "")
      ]
    | map(tostring | gsub("[\\r\\n]+"; " "))
    | @tsv
  ' "$report")
  if [[ -n $failures ]]; then
    printf '  Failures:\n'
    while IFS=$'\t' read -r result level suite id name step path message expected actual; do
      printf '    %s [%s] %s / %s — %s\n' "${result^^}" "$level" "$suite" "$id" "$name"
      [[ -z $step ]] || printf '      Step: %s\n' "$step"
      [[ -z $path ]] || printf '      Path: %s\n' "$path"
      [[ -z $message ]] || printf '      Reason: %s\n' "$message"
      [[ -z $expected ]] || printf '      Expected: %s\n' "$expected"
      [[ -z $actual ]] || printf '      Actual: %s\n' "$actual"
    done <<<"$failures"
  fi

  total=$((total + report_total))
  passed=$((passed + report_passed))
  failed=$((failed + report_failed))
  skipped=$((skipped + report_skipped))
  errors=$((errors + report_errors))
  duration_ms=$((duration_ms + report_duration))
done

if ((${#reports[@]} > 1)); then
  printf '\nCombined summary\n'
  printf '  Total: %s  Passed: %s  Failed: %s  Errors: %s  Skipped: %s  Duration: %s\n' \
    "$total" "$passed" "$failed" "$errors" "$skipped" "$(format_duration "$duration_ms")"
fi

if ((failed > 0 || errors > 0)); then
  exit 1
fi
