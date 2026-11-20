#!/usr/bin/env bash
# Local, debug-friendly entry point for the A2A ITK and ACTS suites.
#
# This delegates to run_itk.sh, the same launcher used in CI, while retaining
# its result files and (by default) enabling the ITK service's DEBUG logs under
# itk/logs/.
set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
revision=${A2A_ITK_REVISION:-main}
acts_run=${ITK_ACTS_RUN:-0}
transports=${ITK_ACTS_TRANSPORTS:-jsonrpc,grpc,rest}
log_level=${ITK_LOG_LEVEL:-DEBUG}
scenario_file=${SCENARIO_FILE:-}
disable_selinux_labels=false

usage() {
  cat <<'EOF'
Usage: ./run_itk_local.sh [options]

Run the Java SDK's local interoperability suite through the same launcher CI
uses. The default is the shared ITK PR scenario set with DEBUG service logs.

Options:
  --acts                    Run ACTS conformance tests instead of ITK traversal tests.
  --transports LIST         ACTS transports (comma-separated; default: jsonrpc,grpc,rest).
  --scenario FILE           Run an explicit ITK scenario YAML or JSON file (ITK only).
  --revision REF            a2a-itk revision to use (default: main).
  --timeout SECONDS         Maximum time for the ITK /run request.
  --container-runtime NAME  Use docker or podman (default: first working runtime).
  --info                    Use INFO instead of DEBUG logging.
  --selinux-mount-labels    Relabel bind mounts with :Z (default for Podman;
                            changes labels on the checkout, cache, and logs).
  --disable-selinux-labels  Let a local Podman container read the bind mount
                            without SELinux container labels. Use only for a
                            local debugging run when a mounted file is denied.
  -h, --help                Show this help.

Useful output after a run:
  raw_results.json          ITK traversal response
  acts-report-*.json        ACTS conformance reports
  ./summarize_results.sh    Readable summary of ACTS conformance reports
  logs/                     Service logs (when DEBUG logging is enabled)

Examples:
  ./itk/run_itk_local.sh
  ./itk/run_itk_local.sh --scenario /tmp/failing-scenario.yaml
  ./itk/run_itk_local.sh --acts --transports jsonrpc
  ./itk/summarize_results.sh
EOF
}

absolute_file() {
  local file=$1
  if [[ ! -f $file ]]; then
    echo "Scenario file does not exist: $file" >&2
    exit 2
  fi
  cd "$(dirname "$file")" && printf '%s/%s\n' "$PWD" "$(basename "$file")"
}

while (($#)); do
  case $1 in
    --acts)
      acts_run=1
      ;;
    --transports)
      [[ $# -ge 2 ]] || { echo "--transports requires a value" >&2; exit 2; }
      transports=$2
      shift
      ;;
    --scenario)
      [[ $# -ge 2 ]] || { echo "--scenario requires a file" >&2; exit 2; }
      scenario_file=$(absolute_file "$2")
      shift
      ;;
    --revision)
      [[ $# -ge 2 ]] || { echo "--revision requires a ref" >&2; exit 2; }
      revision=$2
      shift
      ;;
    --timeout)
      [[ $# -ge 2 && $2 =~ ^[1-9][0-9]*$ ]] || {
        echo "--timeout requires a positive number of seconds" >&2
        exit 2
      }
      export ITK_RUN_TIMEOUT=$2
      shift
      ;;
    --info)
      log_level=INFO
      ;;
    --container-runtime)
      [[ $# -ge 2 && ( $2 == docker || $2 == podman ) ]] || {
        echo "--container-runtime requires docker or podman" >&2
        exit 2
      }
      export ITK_CONTAINER_RT=$2
      shift
      ;;
    --selinux-mount-labels)
      export ITK_SELINUX_MOUNT_LABEL=1
      ;;
    --disable-selinux-labels)
      disable_selinux_labels=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

if [[ -z ${ITK_CONTAINER_RT:-} ]]; then
  for runtime in docker podman; do
    if command -v "$runtime" >/dev/null 2>&1 && "$runtime" info >/dev/null 2>&1; then
      export ITK_CONTAINER_RT=$runtime
      break
    fi
  done
  if [[ -z ${ITK_CONTAINER_RT:-} ]]; then
    echo "No working Docker or Podman runtime found; check that it is running and accessible." >&2
    exit 1
  fi
fi

# Podman on an SELinux-enforcing host cannot access ordinary bind mounts
# without a container label. The shared driver applies :Z to all its mounts.
# Honor an explicit environment setting or the opt-out below.
if [[ $ITK_CONTAINER_RT == podman && -z ${ITK_SELINUX_MOUNT_LABEL+x} ]] && ! $disable_selinux_labels; then
  export ITK_SELINUX_MOUNT_LABEL=1
fi

export A2A_ITK_REVISION=$revision
export ITK_ACTS_RUN=$acts_run
export ITK_ACTS_TRANSPORTS=$transports
export ITK_LOG_LEVEL=$log_level
if [[ -n $scenario_file ]]; then
  export SCENARIO_FILE=$scenario_file
fi

# The shared driver deliberately accepts this as an array. Source its shim
# below so that the optional argument survives; Bash cannot export arrays to a
# child process. `label=disable` avoids altering the checkout's labels, unlike
# a :z/:Z bind-mount suffix, and is only enabled on explicit request.
if $disable_selinux_labels; then
  export ITK_SELINUX_MOUNT_LABEL=0
  ITK_EXTRA_DOCKER_ARGS=(--security-opt label=disable)
fi

cd "$script_dir"
if [[ $acts_run == 1 || ${acts_run^^} == TRUE ]]; then
  suite=ACTS
else
  suite=ITK
fi
echo "Running $suite with $ITK_CONTAINER_RT and a2a-itk revision '$A2A_ITK_REVISION' (logs: $ITK_LOG_LEVEL)"
source ./run_itk.sh
