#!/bin/bash
# Simple loop test to check for cumulative slowdown

cd /Users/kabir/sourcecontrol/AI/a2aproject/a2a-tck

echo "Running single TCK test 20 times to check for cumulative issues..."
echo "Test: test_send_message_request_required_fields"
echo "Press Ctrl+C to stop"
echo ""

for i in {1..20}; do
    echo "=== Run $i/20 ==="
    start_time=$(date +%s)

    .venv/bin/python3 -m pytest \
        tests/required/test_send_message.py::TestMessageSendParams::test_send_message_request_required_fields \
        --sut-url=http://localhost:9999 \
        --tb=short \
        -q \
        --transport-strategy agent_preferred \
        --transports jsonrpc 2>&1 | grep -E "PASSED|FAILED|ERROR|timeout"

    end_time=$(date +%s)
    duration=$((end_time - start_time))
    echo "Duration: ${duration}s"
    echo ""

    # If test takes >10 seconds, it's probably hung
    if [ $duration -gt 10 ]; then
        echo "⚠️  Test took longer than 10 seconds - possible slowdown detected"
    fi
done

echo ""
echo "Loop test complete. Check if duration increased over time."
