#!/bin/bash
# Run multiple streaming tests to reproduce thread accumulation (macOS compatible)

SUT_URL="${SUT_JSONRPC_URL:-http://localhost:9999}"
COUNT="${1:-10}"

echo "=== Running $COUNT streaming tests ==="
echo "Watch server logs for thread count changes"
echo "Look for 'SSE connection closed by client' messages"
echo ""

for i in $(seq 1 $COUNT); do
  echo "Test $i/$COUNT..."

  # Simulate TCK behavior: connect, receive a few events, disconnect
  curl -s -N -X POST "$SUT_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream" \
    -d "{
      \"jsonrpc\": \"2.0\",
      \"id\": $i,
      \"method\": \"SendStreamingMessage\",
      \"params\": {
        \"message\": {
          \"messageId\": \"test-stream-msg-$i\",
          \"contextId\": \"test-stream-ctx-$i\",
          \"role\": \"ROLE_USER\",
          \"parts\": [{\"text\": \"Stream test message $i\"}]
        }
      }
    }" > /dev/null 2>&1 &

  CURL_PID=$!

  # Wait 2 seconds then kill to simulate early disconnect (like TCK)
  sleep 2
  kill $CURL_PID 2>/dev/null
  wait $CURL_PID 2>/dev/null

  # Brief pause between tests
  sleep 0.5
done

echo ""
echo "=== Completed $COUNT tests ==="
echo "Check server logs for final thread count"
echo ""
