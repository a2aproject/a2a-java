package io.a2a.client;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.config.ClientCallContext;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AClientInvalidStateError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
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

public class Client extends AbstractClient {

    private final ClientConfig clientConfig;
    private final ClientTransport clientTransport;
    private AgentCard agentCard;

    public Client(AgentCard agentCard, ClientConfig clientConfig, ClientTransport clientTransport,
                  List<BiConsumer<ClientEvent, AgentCard>> consumers, Consumer<Throwable> streamingErrorHandler) {
        super(consumers, streamingErrorHandler);
        this.agentCard = agentCard;
        this.clientConfig = clientConfig;
        this.clientTransport = clientTransport;
    }


    @Override
    public void sendMessage(Message request, ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, null, null, context);
    }

    @Override
    public void sendMessage(Message request, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            Consumer<Throwable> streamingErrorHandler, ClientCallContext context) throws A2AClientException {
        MessageSendParams messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    @Override
    public void sendMessage(Message request, PushNotificationConfig pushNotificationConfiguration,
                            Map<String, Object> metatadata, ClientCallContext context) throws A2AClientException {
        MessageSendConfiguration messageSendConfiguration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(clientConfig.getAcceptedOutputModes())
                .blocking(clientConfig.isPolling())
                .historyLength(clientConfig.getHistoryLength())
                .pushNotification(pushNotificationConfiguration)
                .build();

        MessageSendParams messageSendParams = new MessageSendParams.Builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(metatadata)
                .build();

        sendMessage(messageSendParams, null, null, context);
    }

    @Override
    public Task getTask(TaskQueryParams request, ClientCallContext context) throws A2AClientException {
        return clientTransport.getTask(request, context);
    }

    @Override
    public Task cancelTask(TaskIdParams request, ClientCallContext context) throws A2AClientException {
        return clientTransport.cancelTask(request, context);
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig request, ClientCallContext context) throws A2AClientException {
        return clientTransport.setTaskPushNotificationConfiguration(request, context);
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams request, ClientCallContext context) throws A2AClientException {
        return clientTransport.getTaskPushNotificationConfiguration(request, context);
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams request, ClientCallContext context) throws A2AClientException {
        return clientTransport.listTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams request, ClientCallContext context) throws A2AClientException {
        clientTransport.deleteTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void resubscribe(TaskIdParams request, ClientCallContext context) throws A2AClientException {
       resubscribeToTask(request, null, null, context);
    }

    @Override
    public void resubscribe(TaskIdParams request, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                            Consumer<Throwable> streamingErrorHandler, ClientCallContext context) throws A2AClientException {
        resubscribeToTask(request, consumers, streamingErrorHandler, context);
    }

    @Override
    public AgentCard getAgentCard(ClientCallContext context) throws A2AClientException {
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

    private void sendMessage(MessageSendParams messageSendParams, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                             Consumer<Throwable> errorHandler, ClientCallContext context) throws A2AClientException {
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

    private void resubscribeToTask(TaskIdParams request, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                                   Consumer<Throwable> errorHandler, ClientCallContext context) throws A2AClientException {
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

    private Consumer<Throwable> getOverriddenErrorHandler(Consumer<Throwable> errorHandler) {
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

    private void consume(ClientEvent clientEvent, AgentCard agentCard, List<BiConsumer<ClientEvent, AgentCard>> consumers) {
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
        MessageSendConfiguration messageSendConfiguration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(clientConfig.getAcceptedOutputModes())
                .blocking(clientConfig.isPolling())
                .historyLength(clientConfig.getHistoryLength())
                .pushNotification(clientConfig.getPushNotificationConfig())
                .build();

        return new MessageSendParams.Builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(clientConfig.getMetadata())
                .build();
    }
}
