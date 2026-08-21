package org.a2aproject.sdk.extras.multitenancy;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * CDI qualifier for tenant-specific beans.
 * <p>
 * Use this qualifier on {@link org.a2aproject.sdk.server.agentexecution.AgentExecutor}
 * and {@link org.a2aproject.sdk.spec.AgentCard} producer methods to declare per-tenant
 * implementations:
 * <pre>{@code
 * @Produces @Tenant("acme")
 * AgentExecutor acmeExecutor() { return new AcmeAgentExecutor(); }
 *
 * @Produces @Tenant("acme") @ExtendedAgentCard
 * AgentCard acmeExtendedCard() { return AgentCard.builder()...build(); }
 * }</pre>
 * <p>
 * The {@link #value()} is a binding member — CDI {@code Instance.select()} matches by value.
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, TYPE, METHOD, PARAMETER})
public @interface Tenant {

    /**
     * The tenant identifier.
     *
     * @return the tenant identifier
     */
    String value();

    /**
     * {@link AnnotationLiteral} for programmatic CDI lookups.
     */
    final class Literal extends AnnotationLiteral<Tenant> implements Tenant {
        private final String value;

        /**
         * Creates a new Literal for the given tenant identifier.
         *
         * @param value the tenant identifier
         */
        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
