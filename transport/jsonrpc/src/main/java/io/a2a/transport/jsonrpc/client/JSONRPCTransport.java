package io.a2a.transport.jsonrpc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCMessage;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.transport.jsonrpc.client.sse.SSEEventListener;
import io.a2a.transport.spi.client.Transport;
import io.a2a.util.Utils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class JSONRPCTransport implements Transport {

    private static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskResponse> GET_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<CancelTaskResponse> CANCEL_TASK_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<GetTaskPushNotificationConfigResponse> GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<SetTaskPushNotificationConfigResponse> SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<ListTaskPushNotificationConfigResponse> LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<DeleteTaskPushNotificationConfigResponse> DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE = new TypeReference<>() {};

    private final String agentUrl;
    private final A2AHttpClient httpClient;

    public JSONRPCTransport(String agentUrl) {
        this(agentUrl, new JdkA2AHttpClient());
    }

    public JSONRPCTransport(String agentUrl, A2AHttpClient httpClient) {
        this.agentUrl = agentUrl;
        this.httpClient = httpClient;
    }

    @Override
    public EventKind sendMessage(String requestId, MessageSendParams messageSendParams) throws A2AServerException {
        SendMessageRequest.Builder sendMessageRequestBuilder = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendMessageRequest.METHOD)
                .params(messageSendParams);

        if (requestId != null) {
            sendMessageRequestBuilder.id(requestId);
        }

        SendMessageRequest sendMessageRequest = sendMessageRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(sendMessageRequest);
            return unmarshalResponse(httpResponseBody, SEND_MESSAGE_RESPONSE_REFERENCE).getResult();
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to send message: " + e, e.getCause());
        }
    }

    @Override
    public Task getTask(String requestId, TaskQueryParams taskQueryParams) throws A2AServerException {
        GetTaskRequest.Builder getTaskRequestBuilder = new GetTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskRequest.METHOD)
                .params(taskQueryParams);

        if (requestId != null) {
            getTaskRequestBuilder.id(requestId);
        }

        GetTaskRequest getTaskRequest = getTaskRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(getTaskRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_RESPONSE_REFERENCE).getResult();
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get task: " + e, e.getCause());
        }
    }

    @Override
    public Task cancelTask(String requestId, TaskIdParams taskIdParams) throws A2AServerException {
        CancelTaskRequest.Builder cancelTaskRequestBuilder = new CancelTaskRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(CancelTaskRequest.METHOD)
                .params(taskIdParams);

        if (requestId != null) {
            cancelTaskRequestBuilder.id(requestId);
        }

        CancelTaskRequest cancelTaskRequest = cancelTaskRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(cancelTaskRequest);
            return unmarshalResponse(httpResponseBody, CANCEL_TASK_RESPONSE_REFERENCE).getResult();
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to cancel task: " + e, e.getCause());
        }
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfig(String requestId, GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams) throws A2AServerException {
        GetTaskPushNotificationConfigRequest.Builder getTaskPushNotificationRequestBuilder = new GetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(GetTaskPushNotificationConfigRequest.METHOD)
                .params(getTaskPushNotificationConfigParams);

        if (requestId != null) {
            getTaskPushNotificationRequestBuilder.id(requestId);
        }

        GetTaskPushNotificationConfigRequest getTaskPushNotificationRequest = getTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(getTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, GET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE).getResult();
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to get task push notification config: " + e, e.getCause());
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfig(String requestId, String taskId, PushNotificationConfig pushNotificationConfig) throws A2AServerException {
        SetTaskPushNotificationConfigRequest.Builder setTaskPushNotificationRequestBuilder = new SetTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SetTaskPushNotificationConfigRequest.METHOD)
                .params(new TaskPushNotificationConfig(taskId, pushNotificationConfig));

        if (requestId != null) {
            setTaskPushNotificationRequestBuilder.id(requestId);
        }

        SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = setTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(setTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, SET_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE).getResult();
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to set task push notification config: " + e, e.getCause());
        }
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfig(String requestId, ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams) throws A2AServerException {
        ListTaskPushNotificationConfigRequest.Builder listTaskPushNotificationRequestBuilder = new ListTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(ListTaskPushNotificationConfigRequest.METHOD)
                .params(listTaskPushNotificationConfigParams);

        if (requestId != null) {
            listTaskPushNotificationRequestBuilder.id(requestId);
        }

        ListTaskPushNotificationConfigRequest listTaskPushNotificationRequest = listTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(listTaskPushNotificationRequest);
            return unmarshalResponse(httpResponseBody, LIST_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE).getResult();
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to list task push notification config: " + e, e.getCause());
        }
    }

    @Override
    public void deleteTaskPushNotificationConfig(String requestId, DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams) throws A2AServerException {
        DeleteTaskPushNotificationConfigRequest.Builder deleteTaskPushNotificationRequestBuilder = new DeleteTaskPushNotificationConfigRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(DeleteTaskPushNotificationConfigRequest.METHOD)
                .params(deleteTaskPushNotificationConfigParams);

        if (requestId != null) {
            deleteTaskPushNotificationRequestBuilder.id(requestId);
        }

        DeleteTaskPushNotificationConfigRequest deleteTaskPushNotificationRequest = deleteTaskPushNotificationRequestBuilder.build();

        try {
            String httpResponseBody = sendPostRequest(deleteTaskPushNotificationRequest);
            unmarshalResponse(httpResponseBody, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_RESPONSE_REFERENCE);
        } catch (IOException | InterruptedException e) {
            throw new A2AServerException("Failed to delete task push notification config: " + e, e.getCause());
        }
    }

    @Override
    public void sendStreamingMessage(String requestId, MessageSendParams messageSendParams, Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        SendStreamingMessageRequest.Builder sendStreamingMessageRequestBuilder = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendStreamingMessageRequest.METHOD)
                .params(messageSendParams);

        if (requestId != null) {
            sendStreamingMessageRequestBuilder.id(requestId);
        }

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventHandler, errorHandler, failureHandler);
        SendStreamingMessageRequest sendStreamingMessageRequest = sendStreamingMessageRequestBuilder.build();
        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(sendStreamingMessageRequest);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));

        } catch (IOException e) {
            throw new A2AServerException("Failed to send streaming message request: " + e, e.getCause());
        } catch (InterruptedException e) {
            throw new A2AServerException("Send streaming message request timed out: " + e, e.getCause());
        }
    }

    @Override
    public void resubscribeToTask(String requestId, TaskIdParams taskIdParams, Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) throws A2AServerException {
        TaskResubscriptionRequest.Builder taskResubscriptionRequestBuilder = new TaskResubscriptionRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(TaskResubscriptionRequest.METHOD)
                .params(taskIdParams);

        if (requestId != null) {
            taskResubscriptionRequestBuilder.id(requestId);
        }

        AtomicReference<CompletableFuture<Void>> ref = new AtomicReference<>();
        SSEEventListener sseEventListener = new SSEEventListener(eventHandler, errorHandler, failureHandler);
        TaskResubscriptionRequest taskResubscriptionRequest = taskResubscriptionRequestBuilder.build();
        try {
            A2AHttpClient.PostBuilder builder = createPostBuilder(taskResubscriptionRequest);
            ref.set(builder.postAsyncSSE(
                    msg -> sseEventListener.onMessage(msg, ref.get()),
                    throwable -> sseEventListener.onError(throwable, ref.get()),
                    () -> {
                        // We don't need to do anything special on completion
                    }));

        } catch (IOException e) {
            throw new A2AServerException("Failed to send task resubscription request: " + e, e.getCause());
        } catch (InterruptedException e) {
            throw new A2AServerException("Task resubscription request timed out: " + e, e.getCause());
        }
    }

    private String sendPostRequest(Object value) throws IOException, InterruptedException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(value);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(Object value) throws JsonProcessingException {
        return httpClient.createPost()
                .url(agentUrl)
                .addHeader("Content-Type", "application/json")
                .body(Utils.OBJECT_MAPPER.writeValueAsString(value));

    }

    private <T extends JSONRPCResponse> T unmarshalResponse(String response, TypeReference<T> typeReference)
            throws A2AServerException, JsonProcessingException {
        T value = Utils.unmarshalFrom(response, typeReference);
        JSONRPCError error = value.getError();
        if (error != null) {
            throw new A2AServerException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""), error);
        }
        return value;
    }
}
