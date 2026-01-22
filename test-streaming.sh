#!/bin/bash
# Test script to simulate TCK streaming behavior (macOS compatible)

SUT_URL="${SUT_JSONRPC_URL:-http://localhost:9999}"

echo "=== Testing Streaming Message ==="
echo "Sending to: $SUT_URL"
echo ""

# Start curl in background and kill it after 2 seconds to simulate early disconnect
curl -N -X POST "$SUT_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "SendStreamingMessage",
    "params": {
      "message": {
        "messageId": "test-stream-msg-1",
        "contextId": "test-stream-ctx-1",
        "role": "ROLE_USER",
        "parts": [{"text": "Stream test message"}]
      }
    }
  }' 2>&1 &

CURL_PID=$!

# Wait 2 seconds then kill curl to simulate client disconnect
sleep 2
kill $CURL_PID 2>/dev/null
wait $CURL_PID 2>/dev/null

echo ""
echo "=== Client disconnected (killed curl after 2s) ==="
echo "Check server logs for 'SSE connection closed by client' message"
echo ""
