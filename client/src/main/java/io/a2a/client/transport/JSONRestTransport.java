/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.a2a.client.transport;

import static io.a2a.util.Assert.checkNotNullParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.a2a.client.ClientCallContext;
import io.a2a.client.ClientCallInterceptor;
import io.a2a.client.PayloadAndHeaders;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.http.A2AHttpClient;
import io.a2a.http.A2AHttpResponse;
import io.a2a.http.JdkA2AHttpClient;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.grpc.utils.ProtoUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JSONRestTransport implements ClientTransport {

    private final A2AHttpClient httpClient;
    private final String agentUrl;
    private final List<ClientCallInterceptor> interceptors;
    private AgentCard agentCard;

    public JSONRestTransport(String agentUrl) {
        this(null, null, agentUrl, null);
    }

    public JSONRestTransport(AgentCard agentCard) {
        this(null, agentCard, agentCard.url(), null);
    }

    public JSONRestTransport(A2AHttpClient httpClient, AgentCard agentCard,
            String agentUrl, List<ClientCallInterceptor> interceptors) {
        this.httpClient = httpClient == null ? new JdkA2AHttpClient() : httpClient;
        this.agentCard = agentCard;
        this.agentUrl = agentUrl.endsWith("/") ? agentUrl.substring(0, agentUrl.length() - 1) : agentUrl;
        this.interceptors = interceptors;
    }

    @Override
    public EventKind sendMessage(MessageSendParams messageSendParams, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("messageSendParams", messageSendParams);
        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
        builder.setRequest(ProtoUtils.ToProto.message(messageSendParams.message()));
        if (messageSendParams.configuration() != null) {
            builder.setConfiguration(ProtoUtils.ToProto.messageSendConfiguration(messageSendParams.configuration()));
        }
        if (messageSendParams.metadata() != null) {
            builder.setMetadata(ProtoUtils.ToProto.struct(messageSendParams.metadata()));
        }
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.SendMessageRequest.METHOD, builder.getRequestOrBuilder(),
                agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(agentUrl + "/v1/message:send", payloadAndHeaders);
            System.out.println("Response " + httpResponseBody);
            io.a2a.grpc.SendMessageResponse.Builder responseBuilder = io.a2a.grpc.SendMessageResponse.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            if (responseBuilder.hasMsg()) {
                return ProtoUtils.FromProto.message(responseBuilder.getMsg());
            }
            return ProtoUtils.FromProto.task(responseBuilder.getTask());
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
    }

    @Override
    public void sendMessageStreaming(MessageSendParams request, Consumer<StreamingEventKind> eventConsumer, Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Task getTask(TaskQueryParams taskQueryParams, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskQueryParams", taskQueryParams);
        GetTaskRequest.Builder builder = GetTaskRequest.newBuilder();
        builder.setName("tasks/" + taskQueryParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.SendMessageRequest.METHOD, builder,
                agentCard, context);
        try {
            String url;
            if(taskQueryParams.historyLength() != null) {
                url = agentUrl + String.format("/v1/tasks/%1s?historyLength=%2d", taskQueryParams.id(), taskQueryParams.historyLength());
            } else {
                url = agentUrl + String.format("/v1/tasks/%1s", taskQueryParams.id());
            }
            System.out.println("Getting URL: " + url);
            A2AHttpClient.GetBuilder getBuilder = httpClient.createGet().url(url);
            if (payloadAndHeaders.getHttpHeaders() != null) {
                for (Map.Entry<String, String> entry : payloadAndHeaders.getHttpHeaders().entrySet()) {
                    getBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            A2AHttpResponse response = getBuilder.get();
            if (!response.success()) {
                IOException e = new IOException("Request failed " + response.status());
                throw new A2AClientException("Failed to send message: " + e, e);
            }
            String httpResponseBody = response.body();
            System.out.println("Response " + httpResponseBody);
            io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
    }

    @Override
    public Task cancelTask(TaskIdParams taskIdParams, ClientCallContext context) throws A2AClientException {
        checkNotNullParam("taskIdParams", taskIdParams);
        CancelTaskRequest.Builder builder = CancelTaskRequest.newBuilder();
        builder.setName("tasks/" + taskIdParams.id());
        PayloadAndHeaders payloadAndHeaders = applyInterceptors(io.a2a.spec.SendMessageRequest.METHOD, builder,
                agentCard, context);
        try {
            String httpResponseBody = sendPostRequest(agentUrl + String.format("/v1/tasks/%1s:cancel", taskIdParams.id()), payloadAndHeaders);
            System.out.println("Response " + httpResponseBody);
            io.a2a.grpc.Task.Builder responseBuilder = io.a2a.grpc.Task.newBuilder();
            JsonFormat.parser().merge(httpResponseBody, responseBuilder);
            return ProtoUtils.FromProto.task(responseBuilder);
        } catch (A2AClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to send message: " + e, e);
        }
    }

    @Override
    public TaskPushNotificationConfig setTaskPushNotificationConfiguration(TaskPushNotificationConfig request, ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public TaskPushNotificationConfig getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams request, ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<TaskPushNotificationConfig> listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams request, ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams request, ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void resubscribe(TaskIdParams request, Consumer<StreamingEventKind> eventConsumer,
             Consumer<Throwable> errorConsumer, ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public AgentCard getAgentCard(ClientCallContext context) throws A2AClientException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private PayloadAndHeaders applyInterceptors(String methodName, MessageOrBuilder payload,
            AgentCard agentCard, ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload, getHttpHeaders(clientCallContext));
        if (interceptors != null && !interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHttpHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }

    private String sendPostRequest(String url, PayloadAndHeaders payloadAndHeaders) throws IOException, InterruptedException {
        A2AHttpClient.PostBuilder builder = createPostBuilder(url, payloadAndHeaders);
        A2AHttpResponse response = builder.post();
        if (!response.success()) {
            throw new IOException("Request failed " + response.status());
        }
        return response.body();
    }

    private A2AHttpClient.PostBuilder createPostBuilder(String url, PayloadAndHeaders payloadAndHeaders) throws JsonProcessingException, InvalidProtocolBufferException {
        System.out.println("**************************************************************************");
        System.out.println(JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));
        System.out.println("**************************************************************************");
        A2AHttpClient.PostBuilder postBuilder = httpClient.createPost()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .body(JsonFormat.printer().print((MessageOrBuilder) payloadAndHeaders.getPayload()));

        if (payloadAndHeaders.getHttpHeaders() != null) {
            for (Map.Entry<String, String> entry : payloadAndHeaders.getHttpHeaders().entrySet()) {
                postBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return postBuilder;
    }

    private Map<String, String> getHttpHeaders(ClientCallContext context) {
        return context != null ? context.getHttpHeaders() : null;
    }
}
