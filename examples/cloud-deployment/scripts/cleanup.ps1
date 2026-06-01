<#
.SYNOPSIS
    Tear down the A2A Cloud example Kubernetes deployment.
.DESCRIPTION
    Deletes all deployed resources in reverse order, removes the Kind cluster
    and local registry container. Windows equivalent of cleanup.sh.
.PARAMETER ContainerTool
    Container runtime to use: 'docker' (default) or 'podman'.
.EXAMPLE
    .\cleanup.ps1
    .\cleanup.ps1 -ContainerTool podman
.NOTES
    Requires PowerShell 5.1 or later. Run with:
        Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
    before running this script if your system policy blocks unsigned scripts.
#>
param(
    [ValidateSet("docker", "podman")]
    [string]$ContainerTool = "docker",
    [switch]$Force
)

Write-Host "============================================"
Write-Host "A2A Cloud Deployment - Cleanup Script"
Write-Host "============================================"
Write-Host ""

# Configure Kind to use podman if specified
if ($ContainerTool -eq "podman") {
    $env:KIND_EXPERIMENTAL_PROVIDER = "podman"
}

Write-Host "This will delete all resources in the a2a-demo namespace and the Kind cluster" -ForegroundColor Yellow

if (-not $Force) {
    $reply = Read-Host "Are you sure you want to continue? (y/N)"
    if ($reply -notmatch "^[Yy]$") {
        Write-Host "Cleanup cancelled"
        exit 0
    }
} else {
    Write-Host "Running in forced mode, skipping confirmation." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Deleting A2A Agent..."
kubectl delete -f "..\k8s\05-agent-deployment.yaml" --ignore-not-found=true

Write-Host ""
Write-Host "Deleting ConfigMap..."
kubectl delete -f "..\k8s\04-agent-configmap.yaml" --ignore-not-found=true

Write-Host ""
Write-Host "Deleting Kafka topic..."
kubectl delete -f "..\k8s\03-kafka-topic.yaml" --ignore-not-found=true

Write-Host ""
Write-Host "Deleting Kafka..."
kubectl delete -f "..\k8s\02-kafka.yaml" --ignore-not-found=true

Write-Host ""
Write-Host "Deleting PostgreSQL..."
kubectl delete -f "..\k8s\01-postgres.yaml" --ignore-not-found=true

Write-Host ""
Write-Host "Deleting namespace..."
kubectl delete -f "..\k8s\00-namespace.yaml" --ignore-not-found=true

Write-Host ""
Write-Host "Deleting Kind cluster..."
kind delete cluster

Write-Host ""
Write-Host "Stopping and removing registry container..."
$null = & $ContainerTool stop kind-registry 2>$null
$null = & $ContainerTool rm kind-registry 2>$null

Write-Host ""
Write-Host "Cleanup completed" -ForegroundColor Green
Write-Host ""
Write-Host "Note: Strimzi operator was not removed" -ForegroundColor Yellow
Write-Host "To remove Strimzi operator, run:"
Write-Host "  kubectl delete namespace kafka"
