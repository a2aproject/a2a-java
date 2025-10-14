package io.a2a.client;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.spec.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SyncClient extends Client {

    SyncClient(AgentCard agentCard, ClientConfig clientConfig, ClientTransport clientTransport,
               List<BiConsumer<ClientEvent, AgentCard>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
        super(agentCard, clientConfig, clientTransport, consumers, streamingErrorHandler);
    }

    public static ClientBuilder builder(AgentCard agentCard) {
        return new ClientBuilder(agentCard);
    }

    public void sendMessage(Message request, @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, null, null, context);
    }

    public void sendMessage(Message request, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            Consumer<Throwable> streamingErrorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    public void sendMessage(Message request, PushNotificationConfig pushNotificationConfiguration,
                            Map<String, Object> metatadata, @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendConfiguration messageSendConfiguration = createMessageSendConfiguration(pushNotificationConfiguration);

        MessageSendParams messageSendParams = new MessageSendParams.Builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(metatadata)
                .build();

        sendMessage(messageSendParams, null, null, context);
    }

    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            return clientTransport.getTask(request, context).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to get task: " + e, e);
        }
    }

    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            return clientTransport.cancelTask(request, context).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to cancel task: " + e, e);
        }
    }

    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            return clientTransport.setTaskPushNotificationConfiguration(request, context).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to set task push notification config: " + e, e);
        }
    }

    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            return clientTransport.getTaskPushNotificationConfiguration(request, context).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to get task push notification config: " + e, e);
        }
    }

    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            return clientTransport.listTaskPushNotificationConfigurations(request, context).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to list task push notification configs: " + e, e);
        }
    }

    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        try {
            clientTransport.deleteTaskPushNotificationConfigurations(request, context).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to delete task push notification config: " + e, e);
        }
    }

    public void resubscribe(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
       resubscribeToTask(request, null, null, context);
    }

    @Override
    public void resubscribe(TaskIdParams request, @Nullable List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        resubscribeToTask(request, consumers, streamingErrorHandler, context);
    }

    @Override
    public AgentCard getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        try {
            agentCard = clientTransport.getAgentCard(context).get();
            return agentCard;
        } catch (ExecutionException | InterruptedException e) {
            throw new A2AClientException("Failed to get agend card: " + e, e);
        }
    }

    @Override
    public void close() {
        clientTransport.close();
    }

    private ClientEvent getClientEvent(StreamingEventKind event, ClientTaskManager taskManager) throws A2AClientError {
        if (event instanceof Message message) {
            return new MessageEvent(message);
        } else if (event instanceof Task task) {
            taskManager.saveTaskEvent(task);
            return new TaskEvent(taskManager.getCurrentTask());
        } else if (event instanceof TaskStatusUpdateEvent updateEvent) {
            taskManager.saveTaskEvent(updateEvent);
            return new TaskUpdateEvent(taskManager.getCurrentTask(), updateEvent);
        } else if (event instanceof TaskArtifactUpdateEvent updateEvent) {
            taskManager.saveTaskEvent(updateEvent);
            return new TaskUpdateEvent(taskManager.getCurrentTask(), updateEvent);
        } else {
            throw new A2AClientInvalidStateError("Invalid client event");
        }
    }

    private MessageSendConfiguration createMessageSendConfiguration(@Nullable PushNotificationConfig pushNotificationConfig) {
        return new MessageSendConfiguration.Builder()
                .acceptedOutputModes(clientConfig.getAcceptedOutputModes())
                .blocking(!clientConfig.isPolling())
                .historyLength(clientConfig.getHistoryLength())
                .pushNotificationConfig(pushNotificationConfig)
                .build();
    }

    private void sendMessage(MessageSendParams messageSendParams, @Nullable List<BiConsumer<ClientEvent, AgentCard>> consumers,
                             @Nullable Consumer<Throwable> errorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            try {
                EventKind eventKind = clientTransport.sendMessage(messageSendParams, context).get();
                ClientEvent clientEvent;
                if (eventKind instanceof Task task) {
                    clientEvent = new TaskEvent(task);
                } else {
                    // must be a message
                    clientEvent = new MessageEvent((Message) eventKind);
                }
                consume(clientEvent, agentCard, consumers);

            } catch (InterruptedException | ExecutionException e) {
                if (e.getCause() instanceof A2AClientException) {
                    throw (A2AClientException) e.getCause();
                }

                throw new A2AClientException("Unable to send message", e);
            }
        } else {
            ClientTaskManager tracker = new ClientTaskManager();
            Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(errorHandler);
            Consumer<StreamingEventKind> eventHandler = event -> {
                try {
                    ClientEvent clientEvent = getClientEvent(event, tracker);
                    consume(clientEvent, agentCard, consumers);
                } catch (A2AClientError e) {
                    overriddenErrorHandler.accept(e);
                }
            };
            clientTransport.sendMessageStreaming(messageSendParams, eventHandler, overriddenErrorHandler, context);
        }
    }

    private void resubscribeToTask(TaskIdParams request, @Nullable List<BiConsumer<ClientEvent, AgentCard>> consumers,
                                   @Nullable Consumer<Throwable> errorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            throw new A2AClientException("Client and/or server does not support resubscription");
        }
        ClientTaskManager tracker = new ClientTaskManager();
        Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(errorHandler);
        Consumer<StreamingEventKind> eventHandler = event -> {
            try {
                ClientEvent clientEvent = getClientEvent(event, tracker);
                consume(clientEvent, agentCard, consumers);
            } catch (A2AClientError e) {
                overriddenErrorHandler.accept(e);
            }
        };
        clientTransport.resubscribe(request, eventHandler, overriddenErrorHandler, context);
    }

    private @NonNull Consumer<Throwable> getOverriddenErrorHandler(@Nullable Consumer<Throwable> errorHandler) {
        return e -> {
            if (errorHandler != null) {
                errorHandler.accept(e);
            } else {
                if (getStreamingErrorHandler() != null) {
                    getStreamingErrorHandler().accept(e);
                }
            }
        };
    }

    private void consume(ClientEvent clientEvent, AgentCard agentCard, @Nullable List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        if (consumers != null) {
            // use specified consumers
            for (BiConsumer<ClientEvent, AgentCard> consumer : consumers) {
                consumer.accept(clientEvent, agentCard);
            }
        } else {
            // use configured consumers
            consume(clientEvent, agentCard);
        }
    }

    private MessageSendParams getMessageSendParams(Message request, ClientConfig clientConfig) {
        MessageSendConfiguration messageSendConfiguration = createMessageSendConfiguration(clientConfig.getPushNotificationConfig());

        return new MessageSendParams.Builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(clientConfig.getMetadata())
                .build();
    }
}
