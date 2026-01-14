#!/bin/bash

# Update version across POMs and JBang scripts
# Usage: ./update-version.sh FROM_VERSION TO_VERSION [--dry-run]

set -euo pipefail # Exit on error, unset var, and pipe failure

FROM_VERSION=$1
TO_VERSION=$2

# Validate arguments
if [ -z "$FROM_VERSION" ] || [ -z "$TO_VERSION" ]; then
    echo "❌ Error: Missing version arguments." >&2
    echo "Usage: $0 FROM_VERSION TO_VERSION [--dry-run]" >&2
    echo "Example: $0 0.3.0.Beta1-SNAPSHOT 0.3.0.Beta1" >&2
    exit 1
fi

# Check if TO_VERSION looks like a flag
if [[ "$TO_VERSION" == --* ]]; then
    echo "❌ Error: TO_VERSION cannot be a flag. Did you mean to provide both FROM_VERSION and TO_VERSION?" >&2
    echo "Usage: $0 FROM_VERSION TO_VERSION [--dry-run]" >&2
    echo "Example: $0 0.3.0.Beta1-SNAPSHOT 0.3.0.Beta1" >&2
    exit 1
fi

DRY_RUN=false
if [ "${3:-}" = "--dry-run" ]; then
    DRY_RUN=true
elif [ -n "${3:-}" ]; then
    echo "❌ Error: Invalid third argument. Only '--dry-run' is supported." >&2
    echo "Usage: $0 FROM_VERSION TO_VERSION [--dry-run]" >&2
    exit 1
fi

# Verify we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Run this script from the a2a-java root directory." >&2
    exit 1
fi

echo "🔍 Updating version from $FROM_VERSION → $TO_VERSION"
echo ""

# Find all files to update
POM_FILES=$(find . -type f -name "pom.xml" | sort)
JBANG_FILES=$(find . -type f -name "*.java" -path "*/examples/*" -exec grep -l "//DEPS io.github.a2asdk:" {} \; | sort)

POM_COUNT=$(echo "$POM_FILES" | wc -l | tr -d ' ')
JBANG_COUNT=$(echo "$JBANG_FILES" | wc -l | tr -d ' ')

echo "📄 Found $POM_COUNT pom.xml files"
echo "📄 Found $JBANG_COUNT JBang script files"
echo ""

# Show what will be changed
if [ "$DRY_RUN" = true ]; then
    echo "🔎 DRY RUN - showing what would be changed:"
    echo ""

    echo "=== POM files with version $FROM_VERSION ==="
    for file in $POM_FILES; do
        if grep -q "$FROM_VERSION" "$file"; then
            echo "  📝 $file"
            grep -n "$FROM_VERSION" "$file" | sed 's/^/      /'
        fi
    done
    echo ""

    echo "=== JBang files with version $FROM_VERSION ==="
    for file in $JBANG_FILES; do
        if grep -q "//DEPS io.github.a2asdk:.*:$FROM_VERSION" "$file"; then
            echo "  📝 $file"
            grep -n "//DEPS io.github.a2asdk:.*:$FROM_VERSION" "$file" | sed 's/^/      /'
        fi
    done
    echo ""

    echo "✅ Dry run complete. Run without --dry-run to apply changes."
    exit 0
fi

# Perform actual updates
echo "🔄 Updating files..."
echo ""

UPDATED_POMS=0
UPDATED_JBANGS=0

# Update POM files
echo "Updating pom.xml files..."
for file in $POM_FILES; do
    if grep -q "$FROM_VERSION" "$file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS requires empty string after -i
            sed -i "" -e "s|>$FROM_VERSION<|>$TO_VERSION<|g" "$file"
        else
            # Linux doesn't need it
            sed -i "s|>$FROM_VERSION<|>$TO_VERSION<|g" "$file"
        fi
        echo "  ✅ $file"
        UPDATED_POMS=$((UPDATED_POMS + 1))
    fi
done
echo ""

# Update JBang files
echo "Updating JBang script files..."
for file in $JBANG_FILES; do
    if grep -q "//DEPS io.github.a2asdk:.*:$FROM_VERSION" "$file"; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS requires empty string after -i
            sed -i "" -e "s/\(\/\/DEPS io.github.a2asdk:.*:\)$FROM_VERSION/\1$TO_VERSION/g" "$file"
        else
            # Linux doesn't need it
            sed -i "s/\(\/\/DEPS io.github.a2asdk:.*:\)$FROM_VERSION/\1$TO_VERSION/g" "$file"
        fi
        echo "  ✅ $file"
        UPDATED_JBANGS=$((UPDATED_JBANGS + 1))
    fi
done
echo ""

# Summary
echo "✅ Version update complete!"
echo "   Updated $UPDATED_POMS pom.xml files"
echo "   Updated $UPDATED_JBANGS JBang script files"
echo ""
echo "📋 Next steps:"
echo "   1. Review changes: git diff"
echo "   2. Verify build: mvn clean install"
echo "   3. Commit changes: git commit -am 'chore: release $TO_VERSION'"
