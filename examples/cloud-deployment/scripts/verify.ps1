<#
.SYNOPSIS
    Verify the A2A Cloud example deployment.
.DESCRIPTION
    Checks that all deployed components (namespace, PostgreSQL, Kafka, agent pods,
    and agent service) are healthy. Windows equivalent of verify.sh.
.EXAMPLE
    .\verify.ps1
.NOTES
    Requires PowerShell 5.1 or later. Run with:
        Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
    before running this script if your system policy blocks unsigned scripts.
#>

Write-Host "============================================"
Write-Host "A2A Cloud Deployment - Verification Script"
Write-Host "============================================"
Write-Host ""

# Check namespace exists
Write-Host "Checking namespace..."
$null = kubectl get namespace a2a-demo 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Namespace 'a2a-demo' exists" -ForegroundColor Green
} else {
    Write-Host "✗ Namespace 'a2a-demo' not found" -ForegroundColor Red
    exit 1
}

# Check PostgreSQL
Write-Host ""
Write-Host "Checking PostgreSQL..."
$postgresReady = kubectl get pods -n a2a-demo -l app=postgres `
    -o "jsonpath={.items[0].status.conditions[?(@.type=='Ready')].status}" 2>$null
if ($postgresReady -eq "True") {
    Write-Host "✓ PostgreSQL is ready" -ForegroundColor Green
    kubectl get pods -n a2a-demo -l app=postgres
} else {
    Write-Host "✗ PostgreSQL is not ready (Ready: $postgresReady)" -ForegroundColor Red
    kubectl get pods -n a2a-demo -l app=postgres
}

# Check Kafka
Write-Host ""
Write-Host "Checking Kafka..."
$kafkaReady = kubectl get kafka a2a-kafka -n kafka `
    -o "jsonpath={.status.conditions[?(@.type=='Ready')].status}" 2>$null
if ($kafkaReady -eq "True") {
    Write-Host "✓ Kafka is ready" -ForegroundColor Green
    kubectl get kafka -n kafka
} else {
    Write-Host "⚠ Kafka may not be fully ready (Status: $kafkaReady)" -ForegroundColor Yellow
    kubectl get kafka -n kafka
}

# Check Agent pods
Write-Host ""
Write-Host "Checking A2A Agent pods..."
$agentStatusJson = kubectl get pods -n a2a-demo -l app=a2a-agent `
    -o "jsonpath={range .items[*]}{.status.conditions[?(@.type=='Ready')].status}{'\n'}{end}" 2>$null
$agentReady = ($agentStatusJson -split "`n" | Where-Object { $_ -eq "True" }).Count
$agentTotal = (kubectl get pods -n a2a-demo -l app=a2a-agent `
    -o "jsonpath={range .items[*]}{.metadata.name}{'\n'}{end}" 2>$null `
    | Where-Object { $_ -ne "" }).Count

Write-Host "Total pods: $agentTotal"
Write-Host "Ready pods: $agentReady"

if ($agentReady -ge 2) {
    Write-Host "✓ Agent pods are running" -ForegroundColor Green
    kubectl get pods -n a2a-demo -l app=a2a-agent -o wide
} else {
    Write-Host "⚠ Not all agent pods are ready" -ForegroundColor Yellow
    kubectl get pods -n a2a-demo -l app=a2a-agent -o wide
}

# Check Agent service
Write-Host ""
Write-Host "Checking A2A Agent service..."
$null = kubectl get svc a2a-agent-service -n a2a-demo 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Agent service exists" -ForegroundColor Green
    kubectl get svc a2a-agent-service -n a2a-demo
} else {
    Write-Host "✗ Agent service not found" -ForegroundColor Red
}

# Instructions
Write-Host ""
Write-Host "=========================================="
Write-Host "To test the agent (via NodePort):"
Write-Host ""
Write-Host "  curl.exe http://localhost:8080/.well-known/agent-card.json"
Write-Host ""
Write-Host "To run the test client (demonstrating load balancing):"
Write-Host "  cd ..\server"
Write-Host "  mvn test-compile exec:java -Dexec.classpathScope=test ``"
Write-Host "    -Dexec.mainClass=`"io.a2a.examples.cloud.A2ACloudExampleClient`" ``"
Write-Host "    -Dagent.url=`"http://localhost:8080`""
Write-Host "=========================================="
