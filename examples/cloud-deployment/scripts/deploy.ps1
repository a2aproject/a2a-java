<#
.SYNOPSIS
    Deploy the A2A Cloud example to a local Kubernetes cluster (Kind).
.DESCRIPTION
    Sets up a local Kind cluster with a container registry, PostgreSQL, Kafka (via Strimzi),
    and deploys the A2A agent. Windows equivalent of deploy.sh.
.PARAMETER ContainerTool
    Container runtime to use: 'docker' (default) or 'podman'.
.EXAMPLE
    .\deploy.ps1
    .\deploy.ps1 -ContainerTool podman
.NOTES
    Requires PowerShell 5.1 or later. Run with:
        Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
    before running this script if your system policy blocks unsigned scripts.
#>
param(
    [ValidateSet("docker", "podman")]
    [string]$ContainerTool = "docker"
)

Write-Host "========================================="
Write-Host "A2A Cloud Deployment - Deployment Script"
Write-Host "========================================="
Write-Host ""

Write-Host "Container tool: $ContainerTool"
Write-Host ""

# Configure Kind to use podman if specified
if ($ContainerTool -eq "podman") {
    $env:KIND_EXPERIMENTAL_PROVIDER = "podman"
    Write-Host "Configured Kind to use podman provider"
    Write-Host ""
}

# Check if Kind is installed
if (-not (Get-Command kind -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Kind is not installed" -ForegroundColor Red
    Write-Host "Please install Kind first: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
}

# Check if kubectl is installed
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "Error: kubectl is not installed" -ForegroundColor Red
    Write-Host "Please install kubectl first: https://kubernetes.io/docs/tasks/tools/"
    exit 1
}

# Setup local registry
Write-Host "Setting up local registry..."
$RegName = "kind-registry"
$RegPort = "5001"

# Create registry container if it doesn't exist
$running = & $ContainerTool inspect -f '{{.State.Running}}' $RegName 2>$null
if ($running -ne "true") {
    Write-Host "Creating registry container..."
    & $ContainerTool run `
        -d --restart=always -p "127.0.0.1:${RegPort}:5000" --network bridge --name $RegName `
        mirror.gcr.io/library/registry:2
    Write-Host "✓ Registry container created" -ForegroundColor Green
} else {
    Write-Host "✓ Registry container already running" -ForegroundColor Green
}

# Create Kind cluster if it doesn't exist
Write-Host ""
$clusters = kind get clusters 2>$null
if ($clusters -notmatch "(?m)^kind$") {
    Write-Host "Creating Kind cluster..."
    kind create cluster --config="..\kind-config.yaml"
    Write-Host "✓ Kind cluster created" -ForegroundColor Green
} else {
    # Check if cluster is healthy by trying to get nodes
    $null = kubectl get nodes 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Existing Kind cluster is not healthy" -ForegroundColor Red
        Write-Host ""
        Write-Host "The cluster exists but is not responding. This usually means:"
        Write-Host "  - The cluster containers are stopped"
        Write-Host "  - The cluster is in a corrupted state"
        Write-Host ""
        Write-Host "To fix this, delete the cluster and re-run this script:"
        Write-Host "  kind delete cluster"
        Write-Host "  .\deploy.ps1"
        Write-Host ""
        exit 1
    } else {
        Write-Host "✓ Kind cluster already exists and is healthy" -ForegroundColor Green
    }
}

# Configure registry on cluster nodes
Write-Host ""
Write-Host "Configuring registry on cluster nodes..."
$RegistryDir = "/etc/containerd/certs.d/localhost:${RegPort}"
foreach ($node in (kind get nodes)) {
    & $ContainerTool exec $node mkdir -p $RegistryDir
    "[host.`"http://${RegName}:5000`"]" | & $ContainerTool exec -i $node sh -c "cat > ${RegistryDir}/hosts.toml"
}
Write-Host "✓ Registry configured on nodes" -ForegroundColor Green

# Connect registry to cluster network
Write-Host ""
Write-Host "Connecting registry to cluster network..."
$networkInfo = & $ContainerTool inspect -f '{{json .NetworkSettings.Networks.kind}}' $RegName 2>$null
if ($networkInfo -eq "null" -or [string]::IsNullOrEmpty($networkInfo)) {
    & $ContainerTool network connect "kind" $RegName
    Write-Host "✓ Registry connected to cluster network" -ForegroundColor Green
} else {
    Write-Host "✓ Registry already connected" -ForegroundColor Green
}

# Create ConfigMap to document local registry
Write-Host ""
Write-Host "Creating registry ConfigMap..."
@"
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:${RegPort}"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
"@ | kubectl apply -f -
Write-Host "✓ Registry ConfigMap created" -ForegroundColor Green

# Verify registry is accessible
Write-Host ""
Write-Host "Verifying registry is accessible..."
$null = curl.exe -s "http://localhost:${RegPort}/v2/" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Registry accessible at localhost:${RegPort}" -ForegroundColor Green
} else {
    Write-Host "ERROR: Registry not accessible" -ForegroundColor Red
    exit 1
}

# Build the project and container image from the server directory
Write-Host ""
Write-Host "Building the project..."
Push-Location "..\server"
try {
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Maven build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Project built successfully" -ForegroundColor Green

    # Build and push container image to local registry
    $Registry = "localhost:${RegPort}"
    Write-Host ""
    Write-Host "Building container image..."
    & $ContainerTool build -t "${Registry}/a2a-cloud-deployment:latest" .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Container image build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Container image built" -ForegroundColor Green

    Write-Host "Pushing image to local registry..."
    if ($ContainerTool -eq "podman") {
        & $ContainerTool push --tls-verify=false "${Registry}/a2a-cloud-deployment:latest"
    } else {
        & $ContainerTool push "${Registry}/a2a-cloud-deployment:latest"
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Image push failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Image pushed to registry" -ForegroundColor Green
} finally {
    Pop-Location
}

# Install Strimzi operator if not already installed
Write-Host ""
Write-Host "Checking for Strimzi operator..."

# Ensure kafka namespace exists
$null = kubectl get namespace kafka 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating kafka namespace..."
    kubectl create namespace kafka
}

$null = kubectl get crd kafkas.kafka.strimzi.io 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Installing Strimzi operator..."
    kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

    Write-Host "Waiting for Strimzi operator deployment to be created..."
    for ($i = 1; $i -le 30; $i++) {
        $null = kubectl get deployment strimzi-cluster-operator -n kafka 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Deployment found"
            break
        }
        if ($i -eq 30) {
            Write-Host "ERROR: Deployment not found after 30 seconds" -ForegroundColor Red
            exit 1
        }
        Start-Sleep -Seconds 1
    }

    Write-Host "Waiting for Strimzi operator to be ready..."
    kubectl wait --for=condition=Available deployment/strimzi-cluster-operator -n kafka --timeout=300s
    kubectl wait --for=condition=Ready pod -l name=strimzi-cluster-operator -n kafka --timeout=300s
    Write-Host "✓ Strimzi operator installed" -ForegroundColor Green
} else {
    Write-Host "✓ Strimzi operator already installed" -ForegroundColor Green
}

# Create namespace
Write-Host ""
Write-Host "Creating namespace..."
kubectl apply -f "..\k8s\00-namespace.yaml"
Write-Host "✓ Namespace created" -ForegroundColor Green

# Deploy PostgreSQL
Write-Host ""
Write-Host "Deploying PostgreSQL..."
kubectl apply -f "..\k8s\01-postgres.yaml"
Write-Host "Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=Ready pod -l app=postgres -n a2a-demo --timeout=120s
Write-Host "✓ PostgreSQL deployed" -ForegroundColor Green

# Deploy Kafka
Write-Host ""
Write-Host "Deploying Kafka..."
kubectl apply -f "..\k8s\02-kafka.yaml"
Write-Host "Waiting for Kafka to be ready (using KRaft mode, typically 2-3 minutes. Timeout is 10 minutes)..."

if ($env:SKIP_ENTITY_OPERATOR_WAIT -eq "true") {
    Write-Host "⚠ SKIP_ENTITY_OPERATOR_WAIT is set - checking broker pod only" -ForegroundColor Yellow

    for ($i = 1; $i -le 60; $i++) {
        Write-Host "Checking Kafka broker status (attempt $i/60)..."
        kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka 2>$null

        $null = kubectl wait --for=condition=Ready pod/a2a-kafka-broker-0 -n kafka --timeout=5s 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Kafka broker pod is ready" -ForegroundColor Green
            Write-Host "⚠ Entity operator may not be ready, but this does not affect functionality" -ForegroundColor Yellow
            break
        }

        if ($i -eq 60) {
            Write-Host "ERROR: Timeout waiting for Kafka broker" -ForegroundColor Red
            kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka
            kubectl describe pod a2a-kafka-broker-0 -n kafka 2>$null
            exit 1
        }

        Start-Sleep -Seconds 5
    }
} else {
    Write-Host "⚠ If waiting for Kafka times out, run .\cleanup.ps1 and retry with: `$env:SKIP_ENTITY_OPERATOR_WAIT = 'true'" -ForegroundColor Yellow

    for ($i = 1; $i -le 60; $i++) {
        Write-Host "Checking Kafka status (attempt $i/60)..."
        kubectl get kafka -n kafka -o wide 2>$null
        kubectl get pods -n kafka -l strimzi.io/cluster=a2a-kafka 2>$null

        $null = kubectl wait --for=condition=Ready kafka/a2a-kafka -n kafka --timeout=10s 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Kafka deployed" -ForegroundColor Green
            break
        }

        if ($i -eq 60) {
            Write-Host "ERROR: Timeout waiting for Kafka" -ForegroundColor Red
            kubectl describe kafka/a2a-kafka -n kafka
            kubectl get events -n kafka --sort-by='.lastTimestamp'
            exit 1
        }
    }
}

# Create Kafka Topic for event replication
Write-Host ""
Write-Host "Creating Kafka topic for event replication..."
kubectl apply -f "..\k8s\03-kafka-topic.yaml"

if ($env:SKIP_ENTITY_OPERATOR_WAIT -eq "true") {
    Write-Host "⚠ SKIP_ENTITY_OPERATOR_WAIT is set - polling Kafka broker for topic" -ForegroundColor Yellow
    Write-Host "  Topic operator may not be ready, waiting for broker to create topic. This check can take several minutes..."

    for ($i = 1; $i -le 30; $i++) {
        $topics = kubectl exec a2a-kafka-broker-0 -n kafka -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092 2>$null
        if ($topics -match "a2a-replicated-events") {
            Write-Host "✓ Topic exists in Kafka broker" -ForegroundColor Green
            break
        }
        if ($i -eq 30) {
            Write-Host "ERROR: Topic not found in broker after 30 attempts" -ForegroundColor Red
            exit 1
        }
        Start-Sleep -Seconds 2
    }
} else {
    Write-Host "Waiting for Kafka topic to be ready..."
    kubectl wait --for=condition=Ready kafkatopic/a2a-replicated-events -n kafka --timeout=60s
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Kafka topic created" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Timeout waiting for Kafka topic" -ForegroundColor Red
        Write-Host "⚠ The topic operator may not be ready in this environment." -ForegroundColor Yellow
        Write-Host "⚠ Run .\cleanup.ps1, then retry with: `$env:SKIP_ENTITY_OPERATOR_WAIT = 'true'" -ForegroundColor Yellow
        exit 1
    }
}

# Deploy Agent ConfigMap
Write-Host ""
Write-Host "Deploying Agent ConfigMap..."
kubectl apply -f "..\k8s\04-agent-configmap.yaml"
Write-Host "✓ ConfigMap deployed" -ForegroundColor Green

# Deploy Agent
if ($env:SKIP_AGENT_DEPLOY -ne "true") {
    Write-Host ""
    Write-Host "Deploying A2A Agent..."
    kubectl apply -f "..\k8s\05-agent-deployment.yaml"

    Write-Host "Waiting for Agent pods to be ready..."
    kubectl wait --for=condition=Ready pod -l app=a2a-agent -n a2a-demo --timeout=120s
    Write-Host "✓ Agent deployed" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "⚠ Skipping agent deployment (SKIP_AGENT_DEPLOY=true)" -ForegroundColor Yellow
    Write-Host "  ConfigMap has been deployed, you can manually deploy the agent with:"
    Write-Host "    kubectl apply -f ..\k8s\05-agent-deployment.yaml"
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "Deployment completed successfully!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "To verify the deployment, run:"
Write-Host "  .\verify.ps1"
Write-Host ""
Write-Host "To access the agent (via NodePort):"
Write-Host "  curl.exe http://localhost:8080/.well-known/agent-card.json"
Write-Host ""
Write-Host "To run the test client (demonstrating load balancing):"
Write-Host "  cd ..\server"
Write-Host "  mvn test-compile exec:java -Dexec.classpathScope=test ``"
Write-Host "    -Dexec.mainClass=`"io.a2a.examples.cloud.A2ACloudExampleClient`" ``"
Write-Host "    -Dagent.url=`"http://localhost:8080`""
