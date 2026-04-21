package org.a2aproject.sdk.compat03.client;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.client.config.ClientConfig_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientInvalidStateError_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Client_v0_3 extends AbstractClient_v0_3 {

    private final ClientConfig_v0_3 clientConfig;
    private final ClientTransport_v0_3 clientTransport;
    private AgentCard_v0_3 agentCard;

    Client_v0_3(AgentCard_v0_3 agentCard, ClientConfig_v0_3 clientConfig, ClientTransport_v0_3 clientTransport,
                List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
        super(consumers, streamingErrorHandler);
        checkNotNullParam("agentCard", agentCard);

        this.agentCard = agentCard;
        this.clientConfig = clientConfig;
        this.clientTransport = clientTransport;
    }

    public static ClientBuilder_v0_3 builder(AgentCard_v0_3 agentCard) {
        return new ClientBuilder_v0_3(agentCard);
    }

    @Override
    public void sendMessage(Message_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        MessageSendParams_v0_3 messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, null, null, context);
    }

    @Override
    public void sendMessage(Message_v0_3 request, List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                            Consumer<Throwable> streamingErrorHandler, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        MessageSendParams_v0_3 messageSendParams = getMessageSendParams(request, clientConfig);
        sendMessage(messageSendParams, consumers, streamingErrorHandler, context);
    }

    @Override
    public void sendMessage(Message_v0_3 request, PushNotificationConfig_v0_3 pushNotificationConfiguration,
                            Map<String, Object> metatadata, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        MessageSendConfiguration_v0_3 messageSendConfiguration = createMessageSendConfiguration(pushNotificationConfiguration);

        MessageSendParams_v0_3 messageSendParams = new MessageSendParams_v0_3.Builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(metatadata)
                .build();

        sendMessage(messageSendParams, null, null, context);
    }

    @Override
    public Task_v0_3 getTask(TaskQueryParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        return clientTransport.getTask(request, context);
    }

    @Override
    public Task_v0_3 cancelTask(TaskIdParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        return clientTransport.cancelTask(request, context);
    }

    @Override
    public TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(
            TaskPushNotificationConfig_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        return clientTransport.setTaskPushNotificationConfiguration(request, context);
    }

    @Override
    public TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(
            GetTaskPushNotificationConfigParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        return clientTransport.getTaskPushNotificationConfiguration(request, context);
    }

    @Override
    public List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(
            ListTaskPushNotificationConfigParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        return clientTransport.listTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(
            DeleteTaskPushNotificationConfigParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        clientTransport.deleteTaskPushNotificationConfigurations(request, context);
    }

    @Override
    public void resubscribe(TaskIdParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
       resubscribeToTask(request, null, null, context);
    }

    @Override
    public void resubscribe(TaskIdParams_v0_3 request, @Nullable List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                            @Nullable Consumer<Throwable> streamingErrorHandler, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        resubscribeToTask(request, consumers, streamingErrorHandler, context);
    }

    @Override
    public AgentCard_v0_3 getAgentCard(@Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        agentCard = clientTransport.getAgentCard(context);
        return agentCard;
    }

    @Override
    public void close() {
        clientTransport.close();
    }

    private ClientEvent_v0_3 getClientEvent(StreamingEventKind_v0_3 event, ClientTaskManager_v0_3 taskManager) throws A2AClientError_v0_3 {
        if (event instanceof Message_v0_3 message) {
            return new MessageEvent_v0_3(message);
        } else if (event instanceof Task_v0_3 task) {
            taskManager.saveTaskEvent(task);
            return new TaskEvent_v0_3(taskManager.getCurrentTask());
        } else if (event instanceof TaskStatusUpdateEvent_v0_3 updateEvent) {
            taskManager.saveTaskEvent(updateEvent);
            return new TaskUpdateEvent_v0_3(taskManager.getCurrentTask(), updateEvent);
        } else if (event instanceof TaskArtifactUpdateEvent_v0_3 updateEvent) {
            taskManager.saveTaskEvent(updateEvent);
            return new TaskUpdateEvent_v0_3(taskManager.getCurrentTask(), updateEvent);
        } else {
            throw new A2AClientInvalidStateError_v0_3("Invalid client event");
        }
    }

    private MessageSendConfiguration_v0_3 createMessageSendConfiguration(@Nullable PushNotificationConfig_v0_3 pushNotificationConfig) {
        return new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(clientConfig.getAcceptedOutputModes())
                .blocking(!clientConfig.isPolling())
                .historyLength(clientConfig.getHistoryLength())
                .pushNotificationConfig(pushNotificationConfig)
                .build();
    }

    private void sendMessage(MessageSendParams_v0_3 messageSendParams, @Nullable List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                             @Nullable Consumer<Throwable> errorHandler, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            EventKind_v0_3 eventKind = clientTransport.sendMessage(messageSendParams, context);
            ClientEvent_v0_3 clientEvent;
            if (eventKind instanceof Task_v0_3 task) {
                clientEvent = new TaskEvent_v0_3(task);
            } else {
                // must be a message
                clientEvent = new MessageEvent_v0_3((Message_v0_3) eventKind);
            }
            consume(clientEvent, agentCard, consumers);
        } else {
            ClientTaskManager_v0_3 tracker = new ClientTaskManager_v0_3();
            Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(errorHandler);
            Consumer<StreamingEventKind_v0_3> eventHandler = event -> {
                try {
                    ClientEvent_v0_3 clientEvent = getClientEvent(event, tracker);
                    consume(clientEvent, agentCard, consumers);
                } catch (A2AClientError_v0_3 e) {
                    overriddenErrorHandler.accept(e);
                }
            };
            clientTransport.sendMessageStreaming(messageSendParams, eventHandler, overriddenErrorHandler, context);
        }
    }

    private void resubscribeToTask(TaskIdParams_v0_3 request, @Nullable List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers,
                                   @Nullable Consumer<Throwable> errorHandler, @Nullable ClientCallContext_v0_3 context) throws A2AClientException_v0_3 {
        if (! clientConfig.isStreaming() || ! agentCard.capabilities().streaming()) {
            throw new A2AClientException_v0_3("Client and/or server does not support resubscription");
        }
        ClientTaskManager_v0_3 tracker = new ClientTaskManager_v0_3();
        Consumer<Throwable> overriddenErrorHandler = getOverriddenErrorHandler(errorHandler);
        Consumer<StreamingEventKind_v0_3> eventHandler = event -> {
            try {
                ClientEvent_v0_3 clientEvent = getClientEvent(event, tracker);
                consume(clientEvent, agentCard, consumers);
            } catch (A2AClientError_v0_3 e) {
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

    private void consume(ClientEvent_v0_3 clientEvent, AgentCard_v0_3 agentCard, @Nullable List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers) {
        if (consumers != null) {
            // use specified consumers
            for (BiConsumer<ClientEvent_v0_3, AgentCard_v0_3> consumer : consumers) {
                consumer.accept(clientEvent, agentCard);
            }
        } else {
            // use configured consumers
            consume(clientEvent, agentCard);
        }
    }

    private MessageSendParams_v0_3 getMessageSendParams(Message_v0_3 request, ClientConfig_v0_3 clientConfig) {
        MessageSendConfiguration_v0_3 messageSendConfiguration = createMessageSendConfiguration(clientConfig.getPushNotificationConfig());

        return new MessageSendParams_v0_3.Builder()
                .message(request)
                .configuration(messageSendConfiguration)
                .metadata(clientConfig.getMetadata())
                .build();
    }
}
