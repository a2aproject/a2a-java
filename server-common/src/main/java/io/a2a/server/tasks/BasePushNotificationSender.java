package io.a2a.server.tasks;

import static io.a2a.common.A2AHeaders.X_A2A_NOTIFICATION_TOKEN;

import io.a2a.server.http.HttpClientManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.a2a.client.http.HttpClient;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BasePushNotificationSender implements PushNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePushNotificationSender.class);

    private final PushNotificationConfigStore configStore;
    private final HttpClientManager clientManager;

    @Inject
    public BasePushNotificationSender(PushNotificationConfigStore configStore, HttpClientManager clientManager) {
        this.configStore = configStore;
        this.clientManager = clientManager;
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
        final String url = pushInfo.url();
        final String token = pushInfo.token();

        // Delegate to the HTTP client manager to better manage client's connection pool.
        final HttpClient client = clientManager.getOrCreate(url);
        final URI uri = URI.create(url);
        HttpClient.PostRequestBuilder postBuilder = client.post(uri.getPath());
        if (token != null && !token.isBlank()) {
            postBuilder.addHeader(X_A2A_NOTIFICATION_TOKEN, token);
        }

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
            postBuilder
                    .body(body)
                    .send()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.debug("Error pushing data to " + url + ": {}", e.getMessage(), e);
            return false;
        }
        return true;
    }
}
