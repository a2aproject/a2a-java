package io.a2a.server.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.a2a.http.A2AHttpClient;
import io.a2a.http.JdkA2AHttpClient;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BasePushNotificationSender implements PushNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePushNotificationSender.class);

    private final A2AHttpClient httpClient;
    private final PushNotificationConfigStore configStore;

    @Inject
    public BasePushNotificationSender(PushNotificationConfigStore configStore) {
        this.httpClient = new JdkA2AHttpClient();
        this.configStore = configStore;
    }

    public BasePushNotificationSender(PushNotificationConfigStore configStore, A2AHttpClient httpClient) {
        this.configStore = configStore;
        this.httpClient = httpClient;
    }

    @Override
    public void sendNotification(Task task) {
        List<PushNotificationConfig> pushConfigs = configStore.getInfo(task.getId());
        if (pushConfigs == null || pushConfigs.isEmpty()) {
            return;
        }

        List<CompletableFuture<Boolean>> dispatchResults = pushConfigs
                .stream()
                .map(pushConfig -> dispatch(task, pushConfig))
                .toList();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(dispatchResults.toArray(new CompletableFuture[0]));
        CompletableFuture<Boolean> dispatchResult = allFutures.thenApply(v -> dispatchResults.stream()
                .allMatch(CompletableFuture::join));
        try {
            boolean allSent = dispatchResult.get();
            if (! allSent) {
                LOGGER.warn("Some push notifications failed to send for taskId: " + task.getId());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Some push notifications failed to send for taskId " + task.getId() + ": {}", e.getMessage(), e);
        }
    }

    private CompletableFuture<Boolean> dispatch(Task task, PushNotificationConfig pushInfo) {
        return CompletableFuture.supplyAsync(() -> dispatchNotification(task, pushInfo));
    }

    private boolean dispatchNotification(Task task, PushNotificationConfig pushInfo) {
        String url = pushInfo.url();

        // TODO auth

        String body;
        try {
            body = Utils.OBJECT_MAPPER.writeValueAsString(task);
        } catch (JsonProcessingException e) {
            LOGGER.debug("Error writing value as string: {}", e.getMessage(), e);
            return false;
        } catch (Throwable throwable) {
            LOGGER.debug("Error writing value as string: {}", throwable.getMessage(), throwable);
            return false;
        }

        try {
            httpClient.createPost()
                    .url(url)
                    .body(body)
                    .post();
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Error pushing data to " + url + ": {}", e.getMessage(), e);
            return false;
        }
        return true;
    }
}
