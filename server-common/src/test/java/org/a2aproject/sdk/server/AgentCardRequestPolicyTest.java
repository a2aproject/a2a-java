package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.multitenancy.TenantNotFoundException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentExtension;
import org.a2aproject.sdk.spec.ExtensionSupportRequiredError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.spec.VersionNotSupportedError;
import org.junit.jupiter.api.Test;

class AgentCardRequestPolicyTest {

    @Test
    void operationRequiresFailureForAnUnsatisfiedCapability() {
        assertThrows(IllegalArgumentException.class,
                () -> new AgentCardOperation(card -> false, null));
    }

    @Test
    void sharedOperationsMatchTheirAgentCardCapabilities() {
        AgentCard capabilitiesDisabled = AgentCardResolverTest.card("disabled");
        AgentCard capabilitiesEnabled = AgentCardResolverTest.card("enabled", AgentCapabilities.builder()
                .streaming(true).pushNotifications(true).extendedAgentCard(true).build());

        assertEquals(false, AgentCardOperation.STREAMING.capability().test(capabilitiesDisabled));
        assertEquals(true, AgentCardOperation.STREAMING.capability().test(capabilitiesEnabled));
        assertInstanceOf(UnsupportedOperationError.class, AgentCardOperation.STREAMING.capabilityFailure().get());

        assertEquals(false, AgentCardOperation.PUSH_NOTIFICATIONS.capability().test(capabilitiesDisabled));
        assertEquals(true, AgentCardOperation.PUSH_NOTIFICATIONS.capability().test(capabilitiesEnabled));
        assertInstanceOf(PushNotificationNotSupportedError.class,
                AgentCardOperation.PUSH_NOTIFICATIONS.capabilityFailure().get());

        assertEquals(false, AgentCardOperation.EXTENDED_CARD.capability().test(capabilitiesDisabled));
        assertEquals(true, AgentCardOperation.EXTENDED_CARD.capability().test(capabilitiesEnabled));
        assertInstanceOf(UnsupportedOperationError.class, AgentCardOperation.EXTENDED_CARD.capabilityFailure().get());
    }

    @Test
    void preflightValidatesBeforeCapabilityAndAttachesCanonicalCard() {
        AgentCard card = AgentCardResolverTest.card("public");
        List<String> calls = new ArrayList<>();
        AgentCardRequestPolicy policy = new AgentCardRequestPolicy(AgentCardResolver.of(() -> {
            calls.add("resolve");
            return card;
        }, () -> null, null));
        ServerCallContext context = context("1.0", Set.of());
        EffectiveAgentCard effective = policy.preflight("/acme/", context, selected -> {
            assertSame(card, selected);
            assertNull(context.getEffectiveAgentCard());
            calls.add("validate");
        }, new AgentCardOperation(selected -> {
            assertNull(context.getEffectiveAgentCard());
            calls.add("capability");
            return true;
        }, UnsupportedOperationError::new));

        assertEquals(List.of("resolve", "validate", "capability"), calls);
        assertEquals("acme", effective.tenant());
        assertSame(card, effective.card());
        assertSame(effective, context.getEffectiveAgentCard());
    }

    @Test
    void invalidTenantWinsBeforeResolutionTransportAndVersionValidation() {
        AgentCardRequestPolicy policy = new AgentCardRequestPolicy(AgentCardResolver.of(() -> {
            throw new AssertionError("must not resolve an invalid tenant");
        }, () -> null, null));
        ServerCallContext context = context("2.0", Set.of());
        InvalidParamsError failure = assertThrows(InvalidParamsError.class,
                () -> policy.preflight("acme/other", context, card -> {
                    throw new AssertionError("must not validate an invalid tenant");
                }, AgentCardOperation.none()));
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", failure.getMessage());
        assertNull(context.getEffectiveAgentCard());
        assertThrows(InvalidParamsError.class,
                () -> policy.resolveAndValidateCapability("bad tenant", card -> {}, AgentCardOperation.none()));
        assertThrows(InvalidParamsError.class,
                () -> policy.validateContextAndAttach(AgentCardResolverTest.card("public"), "bad tenant", context));
    }

    @Test
    void unknownTenantRemainsDistinctFromInvalidParams() {
        AgentCardRouter router = tenant -> null;
        AgentCardRequestPolicy policy = new AgentCardRequestPolicy(AgentCardResolver.of(
                () -> AgentCardResolverTest.card("default"), () -> null, router));
        assertThrows(TenantNotFoundException.class,
                () -> policy.preflight("unknown", context("1.0", Set.of()), card -> {}, AgentCardOperation.none()));
    }

    @Test
    void transportFailureWinsOverVersionAndLeavesContextUnattached() {
        AgentCardRequestPolicy policy = policy(AgentCardResolverTest.card("public"));
        ServerCallContext context = context("2.0", Set.of());
        IllegalStateException failure = new IllegalStateException("invalid transport");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> policy.preflight(null, context, card -> { throw failure; }, AgentCardOperation.none())));
        assertNull(context.getEffectiveAgentCard());
    }

    @Test
    void versionWinsOverRequiredExtensionAndCapability() {
        AgentCardRequestPolicy policy = policy(cardWithRequiredExtension());
        ServerCallContext context = context("2.0", Set.of());
        assertThrows(VersionNotSupportedError.class,
                () -> policy.preflight(null, context, card -> {}, disabledOperation()));
        assertNull(context.getEffectiveAgentCard());
    }

    @Test
    void missingExtensionWinsOverCapability() {
        AgentCardRequestPolicy policy = policy(cardWithRequiredExtension());
        ServerCallContext context = context("1.0", Set.of());
        assertThrows(ExtensionSupportRequiredError.class,
                () -> policy.preflight(null, context, card -> {}, disabledOperation()));
        assertNull(context.getEffectiveAgentCard());
    }

    @Test
    void requiredExtensionAllowsSuccessfulAttachmentWhenRequested() {
        AgentCard card = cardWithRequiredExtension();
        ServerCallContext context = context("1.0", Set.of("urn:test:required"));
        EffectiveAgentCard effective = policy(card).preflight(" ", context, selected -> {}, AgentCardOperation.none());
        assertNull(effective.tenant());
        assertSame(card, effective.card());
    }

    @Test
    void capabilityUsesExactTransportFailureFactoriesWithoutAttaching() {
        for (A2AError failure : List.of(new UnsupportedOperationError(null, "stream disabled", null),
                new PushNotificationNotSupportedError(), new UnsupportedOperationError())) {
            ServerCallContext context = context("1.0", Set.of());
            AgentCardOperation operation = new AgentCardOperation(card -> false, () -> failure);
            assertSame(failure, assertThrows(A2AError.class,
                    () -> policy(AgentCardResolverTest.card("public")).preflight(null, context, card -> {}, operation)));
            assertNull(context.getEffectiveAgentCard());
        }
    }

    @Test
    void splitPreflightValidatesCapabilityBeforeContextAndDoesNotResolveAgain() {
        AgentCard card = AgentCardResolverTest.card("public");
        List<String> calls = new ArrayList<>();
        AgentCardRequestPolicy policy = new AgentCardRequestPolicy(AgentCardResolver.of(() -> {
            calls.add("resolve");
            return card;
        }, () -> null, null));
        assertThrows(PushNotificationNotSupportedError.class,
                () -> policy.resolveAndValidateCapability(null, selected -> calls.add("validate"), disabledOperation()));
        assertEquals(List.of("resolve", "validate"), calls);
        calls.clear();
        AgentCard selected = policy.resolveAndValidateCapability("/acme/", ignored -> calls.add("validate"),
                AgentCardOperation.none());
        ServerCallContext invalid = context("2.0", Set.of());
        assertThrows(VersionNotSupportedError.class, () -> policy.validateContextAndAttach(selected, "/acme/", invalid));
        assertNull(invalid.getEffectiveAgentCard());
        ServerCallContext valid = context("1.0", Set.of());
        EffectiveAgentCard effective = policy.validateContextAndAttach(selected, "/acme/", valid);
        assertEquals("acme", effective.tenant());
        assertSame(card, effective.card());
        assertEquals(List.of("resolve", "validate"), calls);
    }

    private static AgentCardRequestPolicy policy(AgentCard card) {
        return new AgentCardRequestPolicy(AgentCardResolver.of(() -> card, () -> null, null));
    }

    private static AgentCardOperation disabledOperation() {
        return new AgentCardOperation(card -> false, PushNotificationNotSupportedError::new);
    }

    private static AgentCard cardWithRequiredExtension() {
        return AgentCardResolverTest.card("required-extension", AgentCapabilities.builder().extensions(List.of(
                new AgentExtension(null, null, true, "urn:test:required"))).build());
    }

    private static ServerCallContext context(String version, Set<String> extensions) {
        return new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), extensions, version);
    }
}
