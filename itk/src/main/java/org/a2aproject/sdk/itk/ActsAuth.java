package org.a2aproject.sdk.itk;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.spec.SecurityRequirement;
import org.a2aproject.sdk.spec.SecurityScheme;
import org.jspecify.annotations.Nullable;

import io.grpc.Metadata;
import io.grpc.Status;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * ACTS authentication support.
 *
 * <p>Five ACTS tests assert that an agent requiring a credential rejects a request that lacks one.
 * A2A conditions that obligation on the agent's own declared requirements, so those tests gate on a
 * card declaring {@code securitySchemes} and {@code securityRequirements} — an agent declaring
 * neither is not violating anything by serving an unauthenticated request. Every ITK agent declares
 * neither by default, because traversal peers dial it with no credential.
 *
 * <p>Enforcement is therefore opt-in, and the ACTS runner turns it on for a separate pass over just
 * those tests. It cannot be on for the main pass: raw steps are sent exactly as written, so an
 * absent {@code Authorization} header means "reject me" in {@code SEC-AUTH-001} and "serve me" in
 * {@code JSONRPC-ENV-001}, and no server can tell those two requests apart.
 *
 * <p>The extended card is the exception, guarded in either mode: A2A §13.3 makes its authentication
 * unconditional, and no traversal scenario fetches one.
 */
final class ActsAuth {

    /**
     * Credentials the ACTS runner presents. Not secrets: the runner attaches the valid one to every
     * abstract operation and offers the insufficient one from {@code SEC-AUTH-002} and
     * {@code SEC-EXTCARD-002}, so a fixture has to recognise both to answer 200 / 403 / 401 as
     * those tests require.
     */
    private static final String VALID_TOKEN = "itk-valid-token";

    private static final String INSUFFICIENT_TOKEN = "itk-insufficient-token";
    private static final String SCHEME_ID = "bearerAuth";

    /**
     * Suffix rather than a whole path: the card is served at the root and under each binding's
     * prefix, and requiring a credential to read it would be circular — A2A §8.2 makes the
     * well-known URL the discovery mechanism and §7.3 has the client learn its schemes from that
     * card. The ITK readiness probe fetches it too.
     */
    private static final String CARD_PATH_SUFFIX = ".well-known/agent-card.json";

    private static final String EXTENDED_CARD_SUFFIX = "/extendedAgentCard";

    private ActsAuth() {
    }

    static boolean enforced() {
        String value = System.getenv("ITK_ACTS_AUTH");
        return value != null && !value.isEmpty();
    }

    /**
     * The schemes the card advertises, declared only when the agent actually enforces them: a card
     * claiming a scheme it does not check would be a lie, and this is what the ACTS
     * {@code authentication} precondition reads to decide whether the {@code SEC-AUTH} tests apply
     * at all.
     */
    static @Nullable Map<String, SecurityScheme> securitySchemes() {
        if (!enforced()) {
            return null;
        }
        return Map.of(
                SCHEME_ID,
                new HTTPAuthSecurityScheme("opaque", "Bearer", "Bearer token presented by the ACTS runner."));
    }

    /**
     * Separate from the schemes because the two mean different things: schemes are what a client
     * <em>may</em> use, requirements are what it <em>must</em>. An agent publishing the first and
     * not the second requires nothing.
     */
    static @Nullable List<SecurityRequirement> securityRequirements() {
        if (!enforced()) {
            return null;
        }
        return List.of(new SecurityRequirement(Map.of(SCHEME_ID, List.of())));
    }

    /** Three outcomes, because the tests distinguish them. */
    private enum Credential {
        VALID,
        INSUFFICIENT,
        UNUSABLE
    }

    private static Credential presented(@Nullable String header) {
        if (header == null) {
            return Credential.UNUSABLE;
        }
        String[] parts = header.split(" ", 2);
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("Bearer")) {
            return Credential.UNUSABLE;
        }
        String token = parts[1].trim();
        if (VALID_TOKEN.equals(token)) {
            return Credential.VALID;
        }
        if (INSUFFICIENT_TOKEN.equals(token)) {
            return Credential.INSUFFICIENT;
        }
        return Credential.UNUSABLE;
    }

    /**
     * Guards the operation endpoints when enforcement is on, and the extended card always.
     *
     * <p>Registered ahead of the reroute handlers so it sees the path the client actually asked
     * for: {@code /rest/extendedAgentCard} is rerouted to {@code /}, and after that there is no
     * longer anything to recognise.
     */
    static void guard(RoutingContext ctx) {
        String path = ctx.request().path();
        if (path.endsWith(CARD_PATH_SUFFIX)) {
            ctx.next();
            return;
        }
        if (!enforced() && !path.endsWith(EXTENDED_CARD_SUFFIX)) {
            ctx.next();
            return;
        }

        switch (presented(ctx.request().getHeader("Authorization"))) {
            case VALID -> ctx.next();
            case INSUFFICIENT -> reject(ctx, 403, "PERMISSION_DENIED", "Token lacks the required scope.");
            case UNUSABLE -> reject(ctx, 401, "UNAUTHENTICATED", "A bearer token is required.");
        }
    }

    /**
     * The same rule over gRPC metadata, so a card claiming an agent-wide requirement is not
     * contradicted by one binding that serves anyone. gRPC keys are lowercase by protocol.
     *
     * <p>Returns the status to fail the call with, or {@code null} to let it through.
     */
    static @Nullable Status grpcRejection(Metadata metadata) {
        if (!enforced()) {
            return null;
        }
        String header = metadata.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
        return switch (presented(header)) {
            case VALID -> null;
            case INSUFFICIENT -> Status.PERMISSION_DENIED.withDescription("Token lacks the required scope.");
            case UNUSABLE -> Status.UNAUTHENTICATED.withDescription("A bearer token is required.");
        };
    }

    /** Answers with the {@code google.rpc.Status} shape A2A §11.6 requires of an error. */
    private static void reject(RoutingContext ctx, int status, String reason, String message) {
        JsonObject errorInfo = new JsonObject()
                .put("@type", "type.googleapis.com/google.rpc.ErrorInfo")
                .put("reason", reason)
                .put("domain", "a2a-protocol.org");
        JsonObject body = new JsonObject()
                .put(
                        "error",
                        new JsonObject()
                                .put("code", status)
                                .put("status", reason)
                                .put("message", message)
                                .put("details", new JsonArray().add(errorInfo)));

        if (status == 401) {
            ctx.response().putHeader("WWW-Authenticate", "Bearer realm=\"a2a\", scheme=\"" + SCHEME_ID + "\"");
        }
        ctx.response()
                .setStatusCode(status)
                .putHeader("Content-Type", "application/json")
                .end(body.encode());
    }
}
