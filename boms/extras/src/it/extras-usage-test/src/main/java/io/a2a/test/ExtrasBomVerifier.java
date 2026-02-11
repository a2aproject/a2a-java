package io.a2a.test;

import io.a2a.bom.test.DynamicBomVerifier;

import java.util.Set;

/**
 * Verifies Extras BOM completeness by attempting to load all discovered classes.
 * - Includes SDK modules + Extras modules (task stores, queue managers, etc.)
 * - Forbids reference/ to prove BOM doesn't leak reference implementation dependencies
 */
public class ExtrasBomVerifier extends DynamicBomVerifier {

    private static final Set<String> EXTRAS_EXCLUSIONS = Set.of(
        "boms/",       // BOM test modules themselves
        "examples/",   // Example applications
        "tck/",        // TCK test suite
        "tests/",      // Integration tests
        "extras/queue-manager-replicated/tests-multi-instance/",   // Test harness applications
        "extras/queue-manager-replicated/tests-single-instance/",   // Test harness applications
        "extras/opentelemetry/integration-tests/"   // Test harness applications
        // Note: extras/ production modules are NOT in this list - we want to verify those classes load
    );

    private static final Set<String> EXTRAS_FORBIDDEN = Set.of(
        "reference/"   // Reference implementations (separate BOM) - must NOT be loadable
    );

    public ExtrasBomVerifier() {
        super(EXTRAS_EXCLUSIONS, EXTRAS_FORBIDDEN);
    }

    public static void main(String[] args) throws Exception {
        new ExtrasBomVerifier().verify();
    }
}
