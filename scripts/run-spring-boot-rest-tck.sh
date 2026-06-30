#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TCK_DIR="${TCK_DIR:-${ROOT_DIR}/a2a-tck}"
SUT_URL="${SUT_URL:-http://localhost:9999}"
SUT_MODULE="${SUT_MODULE:-integrations/spring-boot/server/rest/spring-boot-server-rest-sut}"
SUT_LOG="${SUT_LOG:-${ROOT_DIR}/spring-boot-rest-sut.log}"
TCK_LOG="${TCK_LOG:-${ROOT_DIR}/tck-output.log}"
SUT_PID_FILE="${SUT_PID_FILE:-${ROOT_DIR}/spring-boot-rest-sut.pid}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-120}"
RETRY_INTERVAL_SECONDS="${RETRY_INTERVAL_SECONDS:-2}"

cleanup() {
  if [[ -f "${SUT_PID_FILE}" ]]; then
    kill "$(cat "${SUT_PID_FILE}")" >/dev/null 2>&1 || true
  fi
  pkill -f "spring-boot-server-rest-sut" >/dev/null 2>&1 || true
}

trap cleanup EXIT

if [[ ! -d "${TCK_DIR}" ]]; then
  echo "TCK directory not found: ${TCK_DIR}" >&2
  exit 1
fi

echo "Starting Spring Boot REST SUT from ${SUT_MODULE}"
mvn -B -pl "${SUT_MODULE}" -am package -DskipTests > "${SUT_LOG}" 2>&1

SUT_JAR_PATH="$(find "${ROOT_DIR}/${SUT_MODULE}/target" -maxdepth 1 -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | head -n 1)"
if [[ -z "${SUT_JAR_PATH}" ]]; then
  echo "Could not find SUT jar in ${ROOT_DIR}/${SUT_MODULE}/target" >&2
  exit 1
fi

echo "Launching SUT jar ${SUT_JAR_PATH}"
java -jar "${SUT_JAR_PATH}" --server.port=9999 >> "${SUT_LOG}" 2>&1 &
echo $! > "${SUT_PID_FILE}"

echo "Waiting for ${SUT_URL}/.well-known/agent-card.json"
start_time="$(date +%s)"
expected_status=200

while true; do
  current_time="$(date +%s)"
  elapsed_time="$((current_time - start_time))"

  if [[ "${elapsed_time}" -ge "${STARTUP_TIMEOUT_SECONDS}" ]]; then
    echo "Timeout: server did not respond with status ${expected_status} within ${STARTUP_TIMEOUT_SECONDS} seconds." >&2
    cat "${SUT_LOG}" || true
    exit 1
  fi

  http_status="$(curl --output /dev/null --silent --write-out "%{http_code}" "${SUT_URL}/.well-known/agent-card.json")" || true

  if [[ "${http_status}" == "${expected_status}" ]]; then
    echo "Server is up after ${elapsed_time} seconds."
    break
  fi

  echo "Server not ready (status: ${http_status}). Retrying in ${RETRY_INTERVAL_SECONDS} seconds..."
  sleep "${RETRY_INTERVAL_SECONDS}"
done

echo "Running TCK from ${TCK_DIR}"
(
  cd "${TCK_DIR}"
  set -o pipefail
  uv run ./run_tck.py --sut-host "${SUT_URL}" -v 2>&1 | tee "${TCK_LOG}"
)
