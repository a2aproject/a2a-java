package org.a2aproject.sdk.test;

import org.a2aproject.sdk.bom.test.DynamicBomVerifier;

import java.util.Set;

/**
 * Verifies Reference BOM completeness by attempting to load all discovered classes.
 * - Includes SDK modules + Reference implementation modules
 * - Forbids extras/ to prove BOM doesn't leak extras dependencies
 */
public class ReferenceBomVerifier extends DynamicBomVerifier {

    private static final Set<String> REFERENCE_EXCLUSIONS = Set.of(
        "boms/",               // BOM test modules themselves
        "examples/",           // Example applications
        "itk/",                // Integration Test Kit agent
        "tck/",                // TCK test suite
        "tests/",              // Integration tests
        "test-utils-docker/",  // Test utilities for Docker-based tests
        "compat-0.3/client/",  // Compat 0.3 client modules (part of SDK BOM)
        "compat-0.3/http-client/", // Compat 0.3 HTTP client (part of SDK BOM)
        "compat-0.3/tck/",      // Compat 0.3 TCK (not yet enabled)
        "compat-0.3/tests/"     // Compat 0.3 test utilities
        // Note: reference/ and compat-0.3/reference/ are NOT excluded - we verify those classes load
    );

    private static final Set<String> REFERENCE_FORBIDDEN = Set.of(
        "extras/"      // Extras modules (separate BOM) - must NOT be loadable
    );

    public ReferenceBomVerifier() {
        super(REFERENCE_EXCLUSIONS, REFERENCE_FORBIDDEN);
    }

    public static void main(String[] args) throws Exception {
        new ReferenceBomVerifier().verify();
    }
}
