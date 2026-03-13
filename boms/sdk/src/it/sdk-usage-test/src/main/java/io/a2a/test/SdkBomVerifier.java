package io.a2a.test;

import io.a2a.bom.test.DynamicBomVerifier;

import java.util.Set;

/**
 * Verifies SDK BOM completeness by attempting to load all discovered classes.
 * - Excludes paths not tested at all (boms/, examples/, tck/, tests/)
 * - Forbids paths that must NOT be loadable (extras/, reference/) to prove BOM doesn't leak dependencies
 */
public class SdkBomVerifier extends DynamicBomVerifier {

    private static final Set<String> SDK_EXCLUSIONS = Set.of(
        "boms/",            // BOM test modules themselves
        "examples/",        // Example applications
        "tck/",             // TCK test suite
        "tests/",           // Integration tests
        "test-utils-docker/" // Test utilities for Docker-based tests
    );

    private static final Set<String> SDK_FORBIDDEN = Set.of(
        "extras/",     // Extras modules (separate BOM) - must NOT be loadable
        "reference/"   // Reference implementations (separate BOM) - must NOT be loadable
    );

    public SdkBomVerifier() {
        super(SDK_EXCLUSIONS, SDK_FORBIDDEN);
    }

    public static void main(String[] args) throws Exception {
        new SdkBomVerifier().verify();
    }
}
