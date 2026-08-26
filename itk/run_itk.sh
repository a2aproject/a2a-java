#!/bin/bash
# ITK harness for a2a-java — a thin shim over a2a-itk's shared driver.
#
# Everything that used to live here (runtime detection, image build, container
# start, readiness poll, POST /run, result reporting, nightly metrics) is now
# in a2a-itk/scripts/run_itk_shared.sh, which all five SDK repos share —
# including the podman fallback this repo needs, which is why the shared
# driver has it.
#
# Scenarios come from the shared role-based set in a2a-itk rather than a
# scenarios.json in this repo — see a2a-itk/scenarios/traversal/.
set -e
cd "$(dirname "${BASH_SOURCE[0]}")"

ITK_SDK_NAME=java
ITK_SCENARIO_SET=shared

# No codegen step: protobuf-maven-plugin reads instruction.proto straight out
# of the a2a-itk checkout (`a2a.itk.proto.dir` in itk/pom.xml), so there is
# nothing to copy. And the repo-root mount already exposes itk/, so the second
# bind mount other SDKs use would be redundant here.
ITK_COPY_PROTO=0
ITK_MOUNT_ITK_DIR=0

# --- bootstrap -------------------------------------------------------------
# The shared driver lives in a2a-itk, so the checkout has to exist before it
# can be sourced. CI has already placed it here via actions/checkout; locally
# we clone it from a2aproject/a2a-itk.
: "${A2A_ITK_REVISION:?A2A_ITK_REVISION environment variable must be set}"
if [ ! -d a2a-itk ]; then
  git clone https://github.com/a2aproject/a2a-itk.git a2a-itk
  git -C a2a-itk checkout "$A2A_ITK_REVISION"
fi

source a2a-itk/scripts/run_itk_shared.sh
