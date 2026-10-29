package org.a2aproject.sdk.server.tasks;

import static org.a2aproject.sdk.client.http.A2AHttpClient.APPLICATION_JSON;
import static org.a2aproject.sdk.client.http.A2AHttpClient.CONTENT_TYPE;
import static org.a2aproject.sdk.common.A2AHeaders.X_A2A_NOTIFICATION_TOKEN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.Instrumented;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BasePushNotificationSender implements PushNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePushNotificationSender.class);
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * CDI {@link Named @Named} qualifier ({@value}) for an optional application-provided
     * {@link Executor} used to dispatch push notifications asynchronously.
     *
     * <p>Applications can produce a bean with this name to supply a custom thread pool
     * (e.g. for sizing or monitoring). When an instrumentation module (such as the
     * OpenTelemetry server extra) is present, it produces an
     * {@link Instrumented @Instrumented} {@link Executor} that wraps the application's
     * {@code @Named} executor (or a default pool) with context propagation.
     *
     * <p>Resolution order (see the {@link #BasePushNotificationSender CDI constructor}):
     * <ol>
     *   <li>{@link Instrumented @Instrumented} {@link Executor} (instrumented wrapper)</li>
     *   <li>{@code @Named("a2a.pushNotificationDispatch")} {@link Executor}
     *       (application-provided)</li>
     *   <li>{@link ForkJoinPool#commonPool()} as last resort</li>
     * </ol>
     */
    public static final String DISPATCH_EXECUTOR_NAME = "a2a.pushNotificationDispatch";

    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private A2AHttpClient httpClient;
    private PushNotificationConfigStore configStore;
    private Map<String, PushNotificationPayloadFormatter> formattersByVersion;
    private PushNotificationUrlValidator urlValidator;
    private Executor dispatchExecutor = ForkJoinPool.commonPool();

    @Inject
    private volatile @Nullable Instance<PushNotificationDeliveryObserver> deliveryObservers;


    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */
    @SuppressWarnings("NullAway")
    protected BasePushNotificationSender() {
        // For CDI proxy creation — all fields are overwritten by the @Inject constructor.
        // urlValidator is intentionally non-null so that SSRF protection is active even
        // if an instance is accidentally used before injection completes.
        this.httpClient = null;
        this.configStore = null;
        this.formattersByVersion = Map.of();
        this.urlValidator = new DefaultPushNotificationUrlValidator();
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore,
                                       PushNotificationUrlValidator urlValidator) {
        this.httpClient = A2AHttpClientFactory.create();
        this.configStore = configStore;
        this.formattersByVersion = Map.of();
        this.urlValidator = urlValidator;
    }

    /**
     * CDI injection constructor.
     *
     * <p>The HTTP client is resolved in order of preference:
     * <ol>
     *   <li>A {@link Instrumented @Instrumented} bean (e.g. one
     *       produced by the OpenTelemetry server extra, which wraps the
     *       application's client with trace-context injection)</li>
     *   <li>An unqualified {@link A2AHttpClient} bean provided by the
     *       application (custom mTLS, proxy, authentication, etc.)</li>
     *   <li>{@link A2AHttpClientFactory#create()} as last resort</li>
     * </ol>
     *
     * <p>The dispatch executor is resolved in order of preference:
     * <ol>
     *   <li>An {@link Instrumented @Instrumented} {@link Executor} (e.g. one
     *       produced by the OpenTelemetry server extra, which wraps the
     *       application's executor with context propagation)</li>
     *   <li>A {@link Named @Named}{@code ("a2a.pushNotificationDispatch")}
     *       {@link Executor} provided by the application</li>
     *   <li>{@link ForkJoinPool#commonPool()} as last resort</li>
     * </ol>
     *
     * @param configStore the store for push notification configurations
     * @param formatters optional formatters for protocol-version-specific payloads
     * @param urlValidator validates push notification URLs to prevent SSRF
     * @param httpClient optional CDI-provided HTTP client
     * @param propagatingClient optional context-propagating HTTP client that wraps the
     *                          application's client with observability instrumentation
     * @param instrumentedExecutor optional instrumented executor with context propagation
     * @param dispatchExecutor optional named executor for async dispatch
     */
    @Inject
    public BasePushNotificationSender(PushNotificationConfigStore configStore,
                                       Instance<PushNotificationPayloadFormatter> formatters,
                                       PushNotificationUrlValidator urlValidator,
                                       Instance<A2AHttpClient> httpClient,
                                       @Instrumented Instance<A2AHttpClient> propagatingClient,
                                       @Instrumented Instance<Executor> instrumentedExecutor,
                                       @Named(DISPATCH_EXECUTOR_NAME) Instance<Executor> dispatchExecutor) {
        this.httpClient = resolveHttpClient(propagatingClient, httpClient);
        this.configStore = configStore;
        this.formattersByVersion = toFormatterMap(formatters);
        this.urlValidator = urlValidator;
        this.dispatchExecutor = resolveExecutor(instrumentedExecutor, dispatchExecutor);
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore, A2AHttpClient httpClient,
                                       PushNotificationUrlValidator urlValidator) {
        this.configStore = configStore;
        this.httpClient = httpClient;
        this.formattersByVersion = Map.of();
        this.urlValidator = urlValidator;
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore, A2AHttpClient httpClient,
                                       List<PushNotificationPayloadFormatter> formatters,
                                       PushNotificationUrlValidator urlValidator) {
        this.configStore = configStore;
        this.httpClient = httpClient;
        this.formattersByVersion = toFormatterMap(formatters);
        this.urlValidator = urlValidator;
    }

    private static A2AHttpClient resolveHttpClient(Instance<A2AHttpClient> propagating,
                                                     Instance<A2AHttpClient> plain) {
        if (propagating.isResolvable()) {
            return propagating.get();
        }
        if (plain.isResolvable()) {
            return plain.get();
        }
        return A2AHttpClientFactory.create();
    }

    private static Executor resolveExecutor(Instance<Executor> instrumented,
                                              Instance<Executor> named) {
        if (instrumented.isResolvable()) {
            return instrumented.get();
        }
        if (named.isResolvable()) {
            return named.get();
        }
        return ForkJoinPool.commonPool();
    }

    private static Map<String, PushNotificationPayloadFormatter> toFormatterMap(
            Iterable<PushNotificationPayloadFormatter> formatters) {
        Map<String, PushNotificationPayloadFormatter> map = new HashMap<>();
        for (PushNotificationPayloadFormatter f : formatters) {
            map.put(f.targetVersion(), f);
        }
        return map;
    }

    @Override
    public boolean sendNotification(StreamingEventKind event, @Nullable Task taskSnapshot) {
        String taskId = extractTaskId(event);
        if (taskId == null) {
            throw new IllegalArgumentException("Cannot send push notification: event does not contain taskId");
        }

        List<TaskPushNotificationConfig> configs = new ArrayList<>();
        String nextPageToken = null;
        do {
          ListTaskPushNotificationConfigsResult pageResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId,
              DEFAULT_PAGE_SIZE, nextPageToken == null ? "" : nextPageToken, ""));
          if (!pageResult.configs().isEmpty()) {
            configs.addAll(pageResult.configs());
          }
          nextPageToken = pageResult.nextPageToken();
        } while (nextPageToken != null);

        if (configs.isEmpty()) {
            LOGGER.debug("No push notification configs registered for taskId: {}", taskId);
            return true;
        }

        Map<String, String> versionsByConfigId = configStore.getProtocolVersions(taskId);

        List<CompletableFuture<Boolean>> dispatchResults = configs
                .stream()
                .map(pushConfig -> dispatch(event, taskSnapshot, pushConfig, versionsByConfigId))
                .toList();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(dispatchResults.toArray(new CompletableFuture[0]));
        CompletableFuture<Boolean> dispatchResult = allFutures.thenApply(v -> dispatchResults.stream()
                .allMatch(CompletableFuture::join));
        try {
            boolean allSent = dispatchResult.get();
            if (!allSent) {
                LOGGER.warn("Some push notifications failed to send for taskId: {}", taskId);
            }
            return allSent;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Some push notifications failed to send for taskId {}: {}", taskId, e.getMessage(), e);
            return false;
        } catch (ExecutionException e) {
            LOGGER.warn("Some push notifications failed to send for taskId {}: {}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extracts the task ID from a streaming event.
     *
     * @param event the streaming event
     * @return the task ID, or {@code null} if the event type does not carry one
     *         (e.g. a {@link Message} without a task ID)
     * @throws IllegalStateException if the event is not a recognized
     *         {@link StreamingEventKind} subtype
     */
    protected @Nullable String extractTaskId(StreamingEventKind event) {
        if (event instanceof Task task) {
            return task.id();
        }
        if (event instanceof Message message) {
            return message.taskId();
        }
        if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.taskId();
        }
        if (event instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return artifactUpdate.taskId();
        }
        throw new IllegalStateException("Unknown StreamingEventKind: " + event);
    }

    private CompletableFuture<Boolean> dispatch(StreamingEventKind event,
                                                 @Nullable Task taskSnapshot,
                                                 TaskPushNotificationConfig pushInfo,
                                                 Map<String, String> versionsByConfigId) {
        return CompletableFuture.supplyAsync(
                () -> dispatchNotification(event, taskSnapshot, pushInfo, versionsByConfigId), dispatchExecutor);
    }

    /**
     * Outcome of a single delivery attempt, distinguishing deliberate skips from failures
     * so that observers are not notified when a notification is intentionally not sent.
     */
    private enum DeliveryOutcome {
        /** The HTTP POST was accepted by the destination. */
        SUCCESS,
        /** Delivery failed (rejected, network error, or format error). */
        FAILURE,
        /** Notification intentionally skipped (e.g. version formatter returned {@code null}). */
        SKIPPED
    }

    private boolean dispatchNotification(StreamingEventKind event,
                                          @Nullable Task taskSnapshot,
                                          TaskPushNotificationConfig pushInfo,
                                          Map<String, String> versionsByConfigId) {
        DeliveryOutcome outcome = attemptDelivery(event, taskSnapshot, pushInfo, versionsByConfigId);
        if (outcome != DeliveryOutcome.SKIPPED) {
            notifyDelivery(pushInfo, outcome == DeliveryOutcome.SUCCESS);
        }
        return outcome != DeliveryOutcome.FAILURE;
    }

    private DeliveryOutcome attemptDelivery(StreamingEventKind event,
                                             @Nullable Task taskSnapshot,
                                             TaskPushNotificationConfig pushInfo,
                                             Map<String, String> versionsByConfigId) {
        String url = pushInfo.url();

        try {
            urlValidator.validate(url);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rejecting push notification to {}: {}", url, e.getMessage());
            return DeliveryOutcome.FAILURE;
        }

        String token = pushInfo.token();

        String version = versionsByConfigId.get(pushInfo.id());
        PushNotificationPayloadFormatter formatter = version != null
                ? formattersByVersion.get(version) : null;

        String body;
        if (formatter != null) {
            try {
                body = formatter.formatPayload(event, taskSnapshot);
            } catch (Exception exception) {
                LOGGER.error("Error formatting payload with {} formatter: {}",
                        version, exception.getMessage(), exception);
                return DeliveryOutcome.FAILURE;
            }
            if (body == null) {
                // Formatter explicitly opted out of sending this notification; this is not a
                // delivery failure — observers must not record it as such.
                LOGGER.debug("Formatter for version {} returned null, skipping notification for {}",
                        version, url);
                return DeliveryOutcome.SKIPPED;
            }
        } else {
            try {
                body = JsonUtil.toJsonStreamingEvent(event);
            } catch (Exception exception) {
                LOGGER.error("Error serializing StreamingEventKind to JSON: {}", exception.getMessage(), exception);
                return DeliveryOutcome.FAILURE;
            }
        }

        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .followRedirects(false);
        if (token != null && !token.isBlank()) {
            try {
                rejectCrlf(token, X_A2A_NOTIFICATION_TOKEN);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Rejecting push notification to {}: {}", url, e.getMessage());
                return DeliveryOutcome.FAILURE;
            }
            postBuilder.addHeader(X_A2A_NOTIFICATION_TOKEN, token);
        }
        AuthenticationInfo authentication = pushInfo.authentication();
        if (authentication != null) {
            String credentials = authentication.credentials();
            if (credentials != null) {
                String authorizationHeader;
                try {
                    authorizationHeader = buildAuthorizationHeader(authentication.scheme(), credentials);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Rejecting push notification to {}: {}", url, e.getMessage());
                    return DeliveryOutcome.FAILURE;
                }
                postBuilder.addHeader("Authorization", authorizationHeader);
            }
        }

        try {
            A2AHttpResponse response = postBuilder
                    .url(url)
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .body(body)
                    .post();
            if (!response.success()) {
                LOGGER.warn("Push notification [config={}] to {} failed with HTTP status {}",
                        pushInfo.id(), url, response.status());
                return DeliveryOutcome.FAILURE;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.warn("Push notification [config={}] to {} failed: {}", pushInfo.id(), url, e.getMessage(), e);
            return DeliveryOutcome.FAILURE;
        }
        return DeliveryOutcome.SUCCESS;
    }

    /**
     * Test-only seam for injecting delivery observers without CDI.
     * In production the {@link #deliveryObservers} field is populated by {@code @Inject}.
     */
    void setDeliveryObservers(@Nullable Instance<PushNotificationDeliveryObserver> deliveryObservers) {
        this.deliveryObservers = deliveryObservers;
    }

    private void notifyDelivery(TaskPushNotificationConfig config, boolean success) {
        if (deliveryObservers == null) {
            return;
        }
        try {
            for (PushNotificationDeliveryObserver observer : deliveryObservers) {
                try {
                    observer.onDelivery(config.taskId(), config.id(), success);
                } catch (RuntimeException e) {
                    LOGGER.debug("Push notification delivery observer failed", e);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Could not resolve push notification delivery observers", e);
        }
    }

    /**
     * Builds the Authorization header value for a push notification config.
     *
     * <p>The {@code scheme} and {@code credentials} are client-controlled values that are
     * concatenated directly into the header. Rejecting CR/LF characters here prevents
     * HTTP header injection (CWE-113). The {@link A2AHttpClient} SPI is pluggable, so we
     * cannot rely on every implementation (or the underlying HTTP client) to validate
     * header values.</p>
     *
     * @param scheme the authentication scheme
     * @param credentials the authentication credentials
     * @return the assembled {@code "scheme credentials"} header value
     * @throws IllegalArgumentException if either field contains CR or LF
     */
    private static String buildAuthorizationHeader(String scheme, String credentials) {
        rejectCrlf(scheme, "Authorization scheme");
        rejectCrlf(credentials, "Authorization credentials");
        return scheme + " " + credentials;
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code value} contains CR or LF.
     *
     * <p>Prevents HTTP header injection (CWE-113) for client-controlled header values.</p>
     *
     * @param value non-null string to validate
     * @param label human-readable description of the field, used in the exception message
     */
    private static void rejectCrlf(String value, String label) {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    label + " must not contain CR/LF characters");
        }
    }
}
