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


    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */
    @SuppressWarnings("NullAway")
    protected BasePushNotificationSender() {
        // For CDI proxy creation
        this.httpClient = null;
        this.configStore = null;
        this.formattersByVersion = Map.of();
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore) {
        this.httpClient = A2AHttpClientFactory.create();
        this.configStore = configStore;
        this.formattersByVersion = Map.of();
    }

    @Inject
    public BasePushNotificationSender(PushNotificationConfigStore configStore,
                                       Instance<PushNotificationPayloadFormatter> formatters) {
        this.httpClient = A2AHttpClientFactory.create();
        this.configStore = configStore;
        this.formattersByVersion = new HashMap<>();
        for (PushNotificationPayloadFormatter f : formatters) {
            this.formattersByVersion.put(f.targetVersion(), f);
        }
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore, A2AHttpClient httpClient) {
        this.configStore = configStore;
        this.httpClient = httpClient;
        this.formattersByVersion = Map.of();
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore, A2AHttpClient httpClient,
                                       List<PushNotificationPayloadFormatter> formatters) {
        this.configStore = configStore;
        this.httpClient = httpClient;
        this.formattersByVersion = new HashMap<>();
        for (PushNotificationPayloadFormatter f : formatters) {
            formattersByVersion.put(f.targetVersion(), f);
        }
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
                body = JsonUtil.toJson(event);
            } catch (Throwable throwable) {
                LOGGER.error("Error serializing StreamingEventKind to JSON: {}", throwable.getMessage(), throwable);
                return false;
            }
        }

        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost();
        if (token != null && !token.isBlank()) {
            postBuilder.addHeader(X_A2A_NOTIFICATION_TOKEN, token);
        }
        if (pushInfo.authentication() != null && pushInfo.authentication().credentials() != null) {
            postBuilder.addHeader("Authorization",
                    pushInfo.authentication().scheme() + " " + pushInfo.authentication().credentials());
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
}
