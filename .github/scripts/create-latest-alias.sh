#!/usr/bin/env bash

set -euo pipefail

versions_dir=${1:?Usage: create-latest-alias.sh <versions-dir> <site-dir>}
site_dir=${2:?Usage: create-latest-alias.sh <versions-dir> <site-dir>}
latest_path=""

for version_file in "$versions_dir"/*.yml; do
    candidate=$(awk '
        $1 == "path:" {
            path = $2
            gsub(/^"|"$/, "", path)
        }
        $1 == "defaultVersion:" && $2 == "true" {
            default_version = 1
        }
        END {
            if (default_version && path != "") {
                print path
            }
        }
    ' "$version_file")

    if [[ -n "$candidate" ]]; then
        if [[ -n "$latest_path" ]]; then
            echo "Expected exactly one default documentation version" >&2
            exit 1
        fi
        latest_path="$candidate"
    fi
done

if [[ -z "$latest_path" ]]; then
    echo "No default documentation version found" >&2
    exit 1
fi

if [[ ! -d "$site_dir/$latest_path" ]]; then
    echo "Default documentation version is not present in the generated site: $latest_path" >&2
    exit 1
fi

if [[ -e "$site_dir/latest" || -L "$site_dir/latest" ]]; then
    echo "Generated site already contains a latest alias" >&2
    exit 1
fi

ln -s "$latest_path" "$site_dir/latest"
