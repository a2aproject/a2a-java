package io.a2a.server.tasks;

import static io.a2a.client.http.A2AHttpClient.APPLICATION_JSON;
import static io.a2a.client.http.A2AHttpClient.CONTENT_TYPE;
import static io.a2a.common.A2AHeaders.X_A2A_NOTIFICATION_TOKEN;

import io.a2a.spec.TaskPushNotificationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
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
    }

    @Inject
    public BasePushNotificationSender(PushNotificationConfigStore configStore) {
        this.httpClient = A2AHttpClientFactory.create();
        this.configStore = configStore;
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore, A2AHttpClient httpClient) {
        this.configStore = configStore;
        this.httpClient = httpClient;
    }

    @Override
    public void sendNotification(Task task) {
        List<TaskPushNotificationConfig> configs = new ArrayList<>();
        String nextPageToken = null;
        do {
          ListTaskPushNotificationConfigResult pageResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(task.id(),
              DEFAULT_PAGE_SIZE, nextPageToken, ""));
          if (!pageResult.configs().isEmpty()) {
            configs.addAll(pageResult.configs());
          }
          nextPageToken = pageResult.nextPageToken();
        } while (nextPageToken != null);

        List<CompletableFuture<Boolean>> dispatchResults = configs
                .stream()
                .map(pushConfig -> dispatch(task, pushConfig.pushNotificationConfig()))
                .toList();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(dispatchResults.toArray(new CompletableFuture[0]));
        CompletableFuture<Boolean> dispatchResult = allFutures.thenApply(v -> dispatchResults.stream()
                .allMatch(CompletableFuture::join));
        try {
            boolean allSent = dispatchResult.get();
            if (!allSent) {
                LOGGER.warn("Some push notifications failed to send for taskId: " + task.id());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Some push notifications failed to send for taskId " + task.id() + ": {}", e.getMessage(), e);
        }
    }

    private CompletableFuture<Boolean> dispatch(Task task, PushNotificationConfig pushInfo) {
        return CompletableFuture.supplyAsync(() -> dispatchNotification(task, pushInfo));
    }

    private boolean dispatchNotification(Task task, PushNotificationConfig pushInfo) {
        String url = pushInfo.url();
        String token = pushInfo.token();

        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost();
        if (token != null && !token.isBlank()) {
            postBuilder.addHeader(X_A2A_NOTIFICATION_TOKEN, token);
        }

        String body;
        try {
            body = JsonUtil.toJson(task);
        } catch (Throwable throwable) {
            LOGGER.debug("Error writing value as string: {}", throwable.getMessage(), throwable);
            return false;
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
