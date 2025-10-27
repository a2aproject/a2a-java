#!/bin/bash

echo "============================================"
echo "A2A Cloud Deployment - Verification Script"
echo "============================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check namespace exists
echo "Checking namespace..."
if kubectl get namespace a2a-demo > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Namespace 'a2a-demo' exists${NC}"
else
    echo -e "${RED}✗ Namespace 'a2a-demo' not found${NC}"
    exit 1
fi

# Check PostgreSQL
echo ""
echo "Checking PostgreSQL..."
POSTGRES_READY=$(kubectl get pods -n a2a-demo -l app=postgres -o jsonpath='{.items[0].status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
if [ "$POSTGRES_READY" = "True" ]; then
    echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
    kubectl get pods -n a2a-demo -l app=postgres
else
    echo -e "${RED}✗ PostgreSQL is not ready (Ready: $POSTGRES_READY)${NC}"
    kubectl get pods -n a2a-demo -l app=postgres
fi

# Check Kafka
echo ""
echo "Checking Kafka..."
KAFKA_READY=$(kubectl get kafka a2a-kafka -n kafka -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
if [ "$KAFKA_READY" = "True" ]; then
    echo -e "${GREEN}✓ Kafka is ready${NC}"
    kubectl get kafka -n kafka
else
    echo -e "${YELLOW}⚠ Kafka may not be fully ready (Status: $KAFKA_READY)${NC}"
    kubectl get kafka -n kafka
fi

# Check Agent pods
echo ""
echo "Checking A2A Agent pods..."
AGENT_PODS=$(kubectl get pods -n a2a-demo -l app=a2a-agent -o jsonpath='{.items | length}' 2>/dev/null)
AGENT_READY=$(kubectl get pods -n a2a-demo -l app=a2a-agent -o jsonpath='{range .items[*]}{.status.conditions[?(@.type=="Ready")].status}{"\n"}{end}' 2>/dev/null | grep -c "True")

echo "Total pods: $AGENT_PODS"
echo "Ready pods: $AGENT_READY"

if [ "$AGENT_READY" -ge 2 ]; then
    echo -e "${GREEN}✓ Agent pods are running${NC}"
    kubectl get pods -n a2a-demo -l app=a2a-agent -o wide
else
    echo -e "${YELLOW}⚠ Not all agent pods are ready${NC}"
    kubectl get pods -n a2a-demo -l app=a2a-agent -o wide
fi

# Check Agent service
echo ""
echo "Checking A2A Agent service..."
if kubectl get svc a2a-agent-service -n a2a-demo > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Agent service exists${NC}"
    kubectl get svc a2a-agent-service -n a2a-demo
else
    echo -e "${RED}✗ Agent service not found${NC}"
fi

# Test agent endpoint
echo ""
echo "=========================================="
echo "To test the agent, first get the URL of the service by running:"
echo "  minikube service a2a-agent-service -n a2a-demo --url"
echo ""
echo "Then, you can test the agent endpoint:
"
echo "  curl <SERVICE_URL>/.well-known/agent-card.json"
echo ""
echo "To run the test client (demonstrating load balancing):"
echo "  cd ../server"
echo "  mvn test-compile exec:java -Dexec.classpathScope=test \\"
echo "    -Dexec.mainClass=\"io.a2a.examples.cloud.A2ACloudExampleClient\" \\"
echo "    -Dagent.url=<SERVICE_URL>"
echo "=========================================="
