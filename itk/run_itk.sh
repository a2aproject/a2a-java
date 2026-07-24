#!/bin/bash
set -ex

# Set default log level
export ITK_LOG_LEVEL="${ITK_LOG_LEVEL:-INFO}"

# Detect container runtime (docker or podman)
if command -v docker &> /dev/null; then
  CONTAINER_RT=docker
elif command -v podman &> /dev/null; then
  CONTAINER_RT=podman
else
  echo "Error: neither docker nor podman found"
  exit 1
fi

# Initialize default exit code
RESULT=1

# Cleanup function to be called on exit
cleanup() {
  set +x
  echo "Cleaning up artifacts..."
  $CONTAINER_RT stop itk-service > /dev/null 2>&1 || true
  $CONTAINER_RT rm itk-service > /dev/null 2>&1 || true
  $CONTAINER_RT rmi itk_service > /dev/null 2>&1 || true
  rm -rf a2a-itk > /dev/null 2>&1 || true
  echo "Done. Final exit code: $RESULT"
}

# Register cleanup function to run on script exit
trap cleanup EXIT

# 1. Pull a2a-itk and checkout revision
: "${A2A_ITK_REVISION:?A2A_ITK_REVISION environment variable must be set}"

if [ ! -d "a2a-itk" ]; then
  git clone https://github.com/a2aproject/a2a-itk.git a2a-itk
fi
cd a2a-itk
git fetch origin
git checkout "$A2A_ITK_REVISION"

# Only pull if it's a branch (not a detached HEAD)
if git symbolic-ref -q HEAD > /dev/null; then
  git pull origin "$A2A_ITK_REVISION"
fi
cd ..

# 2. Copy latest instruction.proto from a2a-itk
cp a2a-itk/protos/instruction.proto src/main/proto/instruction.proto

# 3. Build itk_service container image from root of a2a-itk
CONTAINER_BUILD_ARGS=""
if [ "$CONTAINER_RT" = "podman" ]; then
  CONTAINER_BUILD_ARGS="--format docker"
fi
$CONTAINER_RT build $CONTAINER_BUILD_ARGS -t itk_service a2a-itk

# 4. Start container service with a single mount: the a2a-java repo
A2A_JAVA_ROOT=$(cd .. && pwd)

# Stop existing container if any
$CONTAINER_RT rm -f itk-service || true

# Create logs directory if debug
if [ "${ITK_LOG_LEVEL^^}" = "DEBUG" ]; then
  mkdir -p logs
fi

DOCKER_MOUNT_LOGS=""
if [ "${ITK_LOG_LEVEL^^}" = "DEBUG" ]; then
  DOCKER_MOUNT_LOGS="-v $(pwd)/logs:/app/logs"
fi

$CONTAINER_RT run -d --name itk-service \
  -v "$A2A_JAVA_ROOT:/app/agents/repo" \
  $DOCKER_MOUNT_LOGS \
  -e ITK_LOG_LEVEL="$ITK_LOG_LEVEL" \
  -p 8000:8000 \
  itk_service

# 5. Verify service is up and send post request
MAX_RETRIES=30
echo "Waiting for ITK service to start on 127.0.0.1:8000..."
set +e
for i in $(seq 1 $MAX_RETRIES); do
  if curl -s http://127.0.0.1:8000/ > /dev/null; then
    echo "Service is up!"
    break
  fi
  echo "Still waiting... ($i/$MAX_RETRIES)"
  sleep 2
done

# If we reached the end of the loop without success
if ! curl -s http://127.0.0.1:8000/ > /dev/null; then
  echo "Error: ITK service failed to start on port 8000"
  $CONTAINER_RT logs itk-service
  exit 1
fi


SCENARIO_FILE="scenarios.json"
if [ "${ITK_NIGHTLY_RUN^^}" = "TRUE" ]; then
  SCENARIO_FILE="scenarios_full.json"
fi

echo "ITK Service is up! Sending compatibility test request using $SCENARIO_FILE..."
RESPONSE=$(curl -s -X POST http://127.0.0.1:8000/run \
  -H "Content-Type: application/json" \
  -d "@$SCENARIO_FILE")

if [ "${ITK_NIGHTLY_RUN^^}" = "TRUE" ]; then
  echo "Nightly run detected. Saving raw results and running process_results.py..."
  echo "$RESPONSE" > raw_results.json
  python3 a2a-itk/scripts/process_results.py \
    --history_output_file itk_java.json \
    --history_url https://github.com/a2aproject/a2a-java/releases/download/nightly-metrics/itk_java.json
  RESULT=$?
else
  echo "--------------------------------------------------------"
  echo "ITK TEST RESULTS:"
  echo "--------------------------------------------------------"
  echo "$RESPONSE" | python3 -c "
import sys, json
raw = sys.stdin.read()
try:
    data = json.loads(raw)
    all_passed = data.get('all_passed', False)
    results = data.get('results', {})
    for test, passed in results.items():
        status = 'PASSED' if passed else 'FAILED'
        print(f'{test}: {status}')
    print('--------------------------------------------------------')
    print(f'OVERALL STATUS: {\"PASSED\" if all_passed else \"FAILED\"}')
    if not all_passed:
        sys.exit(1)
except Exception as e:
    print(f'Error parsing results: {e}')
    print(f'Raw response: {raw}')
    sys.exit(1)
"
  RESULT=$?
fi
set -e

if [ $RESULT -ne 0 ]; then
  echo "Tests failed. Container logs:"
  $CONTAINER_RT logs itk-service
fi
echo "--------------------------------------------------------"

# Final exit result will be captured by trap cleanup
exit $RESULT
