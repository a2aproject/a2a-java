#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT

versions_dir="$work_dir/versions"
site_dir="$work_dir/site"
mkdir -p "$versions_dir" "$site_dir/1_4_0_Final/apidocs" "$site_dir/1_5_0_Final/apidocs"

cat > "$versions_dir/1.4.0.Final.yml" <<'EOF'
path: "1_4_0_Final"
defaultVersion: false
EOF

cat > "$versions_dir/1.5.0.Final.yml" <<'EOF'
path: "1_5_0_Final"
defaultVersion: true
EOF

"$script_dir/create-latest-alias.sh" "$versions_dir" "$site_dir"

test -L "$site_dir/latest"
test "$(readlink "$site_dir/latest")" = "1_5_0_Final"
test -d "$site_dir/latest/apidocs"
