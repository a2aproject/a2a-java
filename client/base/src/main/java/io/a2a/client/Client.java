package io.a2a.client;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AClientInvalidStateError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskStatusUpdateEvent;

import static io.a2a.util.Assert.checkNotNullParam;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Client extends AbstractClient {

    private final ClientConfig clientConfig;
    private final ClientTransport clientTransport;
    private AgentCard agentCard;

    Client(AgentCard agentCard, ClientConfig clientConfig, ClientTransport clientTransport,
                  List<BiConsumer<ClientEvent, AgentCard>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
        super(consumers, streamingErrorHandler);
        checkNotNullParam("agentCard", agentCard);

        this.agentCard = agentCard;
        this.clientConfig = clientConfig;
        this.clientTransport = clientTransport;
    }

    public static ClientBuilder builder(AgentCard agentCard) {
        return new ClientBuilder(agentCard);
    }

    @Override
    public void sendMessage(@NonNull Message request,
                            @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler,
                            @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    @Override
    public void sendMessage(@NonNull Message request,
                            @Nullable PushNotificationConfig pushNotificationConfiguration,
                            @Nullable Map<String, Object> metadata,
                            @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendConfiguration messageSendConfiguration = createMessageSendConfiguration(pushNotificationConfiguration);

        MessageSendParams messageSendParams = MessageSendParams.builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(metadata)
                .build();

        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.getTask(request, context);
    }

    @Override
    public ListTasksResult listTasks(ListTasksParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.listTasks(request, context);
    }

    @Override
    public Task cancelTask(TaskIdParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.cancelTask(request, context);
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.setTaskPushNotificationConfiguration(request, context);
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.getTaskPushNotificationConfiguration(request, context);
    }

    @Override
    public ListTaskPushNotificationConfigResult listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.listTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        clientTransport.deleteTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void resubscribe(@NonNull TaskIdParams request,
                            @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler,
                            @Nullable ClientCallContext context) throws A2AClientException {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            throw new A2AClientException("Client and/or server does not support resubscription");
        }
        ClientTaskManager tracker = new ClientTaskManager();
        Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(streamingErrorHandler);
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

    @Override
    public AgentCard getAgentCard(@Nullable ClientCallContext context) throws A2AClientException {
        agentCard = clientTransport.getAgentCard(context);
        return agentCard;
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
        return MessageSendConfiguration.builder()
                .acceptedOutputModes(clientConfig.getAcceptedOutputModes())
                .blocking(!clientConfig.isPolling())
                .historyLength(clientConfig.getHistoryLength())
                .pushNotificationConfig(pushNotificationConfig)
                .build();
    }

    private void sendMessage(@NonNull MessageSendParams messageSendParams, @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers,
                             @Nullable Consumer<Throwable> errorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            EventKind eventKind = clientTransport.sendMessage(messageSendParams, context);
            ClientEvent clientEvent;
            if (eventKind instanceof Task task) {
                clientEvent = new TaskEvent(task);
            } else {
                // must be a message
                clientEvent = new MessageEvent((Message) eventKind);
            }
            consume(clientEvent, agentCard, consumers);
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

    private void consume(ClientEvent clientEvent, AgentCard agentCard, @NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        for (BiConsumer<ClientEvent, AgentCard> consumer : consumers) {
            consumer.accept(clientEvent, agentCard);
        }
    }

    private MessageSendParams getMessageSendParams(Message request, ClientConfig clientConfig) {
        MessageSendConfiguration messageSendConfiguration = createMessageSendConfiguration(clientConfig.getPushNotificationConfig());

        return MessageSendParams.builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(clientConfig.getMetadata())
                .build();
    }
}
