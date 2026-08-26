package org.a2aproject.sdk.server.tasks;

import static org.a2aproject.sdk.client.http.A2AHttpClient.APPLICATION_JSON;
import static org.a2aproject.sdk.client.http.A2AHttpClient.CONTENT_TYPE;
import static org.a2aproject.sdk.common.A2AHeaders.X_A2A_NOTIFICATION_TOKEN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
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

    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private A2AHttpClient httpClient;
    private PushNotificationConfigStore configStore;
    private Map<String, PushNotificationPayloadFormatter> formattersByVersion;
    private PushNotificationUrlValidator urlValidator;


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

    @Inject
    public BasePushNotificationSender(PushNotificationConfigStore configStore,
                                       Instance<PushNotificationPayloadFormatter> formatters,
                                       PushNotificationUrlValidator urlValidator) {
        this.httpClient = A2AHttpClientFactory.create();
        this.configStore = configStore;
        this.formattersByVersion = toFormatterMap(formatters);
        this.urlValidator = urlValidator;
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

    private static Map<String, PushNotificationPayloadFormatter> toFormatterMap(
            Iterable<PushNotificationPayloadFormatter> formatters) {
        Map<String, PushNotificationPayloadFormatter> map = new HashMap<>();
        for (PushNotificationPayloadFormatter f : formatters) {
            map.put(f.targetVersion(), f);
        }
        return map;
    }

    @Override
    public void sendNotification(StreamingEventKind event, @Nullable Task taskSnapshot) {
        String taskId = extractTaskId(event);
        if (taskId == null) {
            LOGGER.warn("Cannot send push notification: event does not contain taskId");
            return;
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
                LOGGER.warn("Some push notifications failed to send for taskId: " + taskId);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Some push notifications failed to send for taskId " + taskId + ": {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts the task ID from a StreamingEventKind event.
     *
     * @param event the streaming event
     * @return the task ID, or null if not available
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
        return CompletableFuture.supplyAsync(() -> dispatchNotification(event, taskSnapshot, pushInfo, versionsByConfigId));
    }

    private boolean dispatchNotification(StreamingEventKind event,
                                          @Nullable Task taskSnapshot,
                                          TaskPushNotificationConfig pushInfo,
                                          Map<String, String> versionsByConfigId) {
        String url = pushInfo.url();

        try {
            urlValidator.validate(url);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rejecting push notification to {}: {}", url, e.getMessage());
            return false;
        }

        String token = pushInfo.token();

        String version = versionsByConfigId.get(pushInfo.id());
        PushNotificationPayloadFormatter formatter = version != null
                ? formattersByVersion.get(version) : null;

        String body;
        if (formatter != null) {
            try {
                body = formatter.formatPayload(event, taskSnapshot);
            } catch (Throwable throwable) {
                LOGGER.error("Error formatting payload with {} formatter: {}",
                        version, throwable.getMessage(), throwable);
                return false;
            }
            if (body == null) {
                LOGGER.debug("Formatter for version {} returned null, skipping notification for {}",
                        version, url);
                return true;
            }
        } else {
            try {
                body = JsonUtil.toJsonStreamingEvent(event);
            } catch (Throwable throwable) {
                LOGGER.error("Error serializing StreamingEventKind to JSON: {}", throwable.getMessage(), throwable);
                return false;
            }
        }

        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .followRedirects(false);
        if (token != null && !token.isBlank()) {
            try {
                rejectCrlf(token, X_A2A_NOTIFICATION_TOKEN);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Rejecting push notification to {}: {}", url, e.getMessage());
                return false;
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
                    return false;
                }
                postBuilder.addHeader("Authorization", authorizationHeader);
            }
        }

        try {
            postBuilder
                    .url(url)
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .body(body)
                    .post();
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Error pushing data to " + url + ": {}", e.getMessage(), e);
            return false;
        }
        return true;
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
