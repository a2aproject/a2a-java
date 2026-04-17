package org.a2aproject.sdk.compat03.client;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.client.config.ClientConfig;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.compat03.spec.A2AClientError;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.A2AClientInvalidStateError;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.EventKind;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.Message;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration;
import org.a2aproject.sdk.compat03.spec.MessageSendParams;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.compat03.spec.TaskIdParams;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

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
    public void sendMessage(Message request, @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, null, null, context);
    }

    @Override
    public void sendMessage(Message request, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            Consumer<Throwable> streamingErrorHandler, @Nullable ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    @Override
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

    @Override
    public Task getTask(TaskQueryParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.getTask(request, context);
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
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        return clientTransport.listTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request, @Nullable ClientCallContext context) throws A2AClientException {
        clientTransport.deleteTaskPushNotificationConfigurations(request, context);
    }

    @Override
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
