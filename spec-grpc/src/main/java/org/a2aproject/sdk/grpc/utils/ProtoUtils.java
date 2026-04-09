package org.a2aproject.sdk.grpc.utils;

import java.util.ArrayList;
import java.util.List;

import org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.grpc.StreamResponse;
import org.a2aproject.sdk.grpc.mapper.AgentCardMapper;
import org.a2aproject.sdk.grpc.mapper.DeleteTaskPushNotificationConfigParamsMapper;
import org.a2aproject.sdk.grpc.mapper.GetTaskPushNotificationConfigParamsMapper;
import org.a2aproject.sdk.grpc.mapper.ListTaskPushNotificationConfigsParamsMapper;
import org.a2aproject.sdk.grpc.mapper.ListTasksParamsMapper;
import org.a2aproject.sdk.grpc.mapper.ListTasksResultMapper;
import org.a2aproject.sdk.grpc.mapper.MessageMapper;
import org.a2aproject.sdk.grpc.mapper.MessageSendConfigurationMapper;
import org.a2aproject.sdk.grpc.mapper.MessageSendParamsMapper;
import org.a2aproject.sdk.grpc.mapper.StreamResponseMapper;
import org.a2aproject.sdk.grpc.mapper.TaskArtifactUpdateEventMapper;
import org.a2aproject.sdk.grpc.mapper.TaskIdParamsMapper;
import org.a2aproject.sdk.grpc.mapper.TaskMapper;
import org.a2aproject.sdk.grpc.mapper.TaskPushNotificationConfigMapper;
import org.a2aproject.sdk.grpc.mapper.TaskQueryParamsMapper;
import org.a2aproject.sdk.grpc.mapper.TaskStateMapper;
import org.a2aproject.sdk.grpc.mapper.TaskStatusUpdateEventMapper;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetExtendedAgentCardParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;

/**
 * Utility class to convert between GRPC and Spec objects.
 */
public class ProtoUtils {

    public static class ToProto {

        public static org.a2aproject.sdk.grpc.AgentCard agentCard(AgentCard agentCard) {
            return AgentCardMapper.INSTANCE.toProto(agentCard);
        }

        public static org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest extendedAgentCard(GetExtendedAgentCardParams params) {
            GetExtendedAgentCardRequest.Builder builder = GetExtendedAgentCardRequest.newBuilder();
            if (params.tenant() != null) {
                builder.setTenant(params.tenant());
            }
            return builder.build();
        }

        public static org.a2aproject.sdk.grpc.GetTaskRequest getTaskRequest(TaskQueryParams params) {
            return TaskQueryParamsMapper.INSTANCE.toProto(params);
        }

        public static org.a2aproject.sdk.grpc.CancelTaskRequest cancelTaskRequest(CancelTaskParams params) {
            return TaskIdParamsMapper.INSTANCE.toProtoCancelTaskRequest(params);
        }

        public static org.a2aproject.sdk.grpc.SubscribeToTaskRequest subscribeToTaskRequest(TaskIdParams params) {
            return TaskIdParamsMapper.INSTANCE.toProtoSubscribeToTaskRequest(params);
        }

        public static org.a2aproject.sdk.grpc.TaskPushNotificationConfig createTaskPushNotificationConfigRequest(TaskPushNotificationConfig config) {
            return TaskPushNotificationConfigMapper.INSTANCE.toProto(config);
        }

        public static org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest getTaskPushNotificationConfigRequest(GetTaskPushNotificationConfigParams params) {
            return GetTaskPushNotificationConfigParamsMapper.INSTANCE.toProto(params);
        }

        public static org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest deleteTaskPushNotificationConfigRequest(DeleteTaskPushNotificationConfigParams params) {
            return DeleteTaskPushNotificationConfigParamsMapper.INSTANCE.toProto(params);
        }

        public static org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest listTaskPushNotificationConfigsRequest(ListTaskPushNotificationConfigsParams params) {
            return ListTaskPushNotificationConfigsParamsMapper.INSTANCE.toProto(params);
        }

        public static org.a2aproject.sdk.grpc.Task task(Task task) {
            return TaskMapper.INSTANCE.toProto(task);
        }

        public static org.a2aproject.sdk.grpc.ListTasksResponse listTasksResult(ListTasksResult result) {
            return ListTasksResultMapper.INSTANCE.toProto(result);
        }

        public static org.a2aproject.sdk.grpc.ListTasksRequest listTasksParams(ListTasksParams params) {
            return ListTasksParamsMapper.INSTANCE.toProto(params);
        }

        public static org.a2aproject.sdk.grpc.Message message(Message message) {
            return MessageMapper.INSTANCE.toProto(message);
        }

        public static org.a2aproject.sdk.grpc.TaskPushNotificationConfig taskPushNotificationConfig(TaskPushNotificationConfig config) {
            return TaskPushNotificationConfigMapper.INSTANCE.toProto(config);
        }

        public static org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent taskArtifactUpdateEvent(TaskArtifactUpdateEvent event) {
            return TaskArtifactUpdateEventMapper.INSTANCE.toProto(event);
        }

        public static org.a2aproject.sdk.grpc.TaskStatusUpdateEvent taskStatusUpdateEvent(TaskStatusUpdateEvent event) {
            return TaskStatusUpdateEventMapper.INSTANCE.toProto(event);
        }

        public static org.a2aproject.sdk.grpc.TaskState taskState(TaskState taskState) {
            return TaskStateMapper.INSTANCE.toProto(taskState);
        }

        public static org.a2aproject.sdk.grpc.SendMessageConfiguration messageSendConfiguration(MessageSendConfiguration messageSendConfiguration) {
            return MessageSendConfigurationMapper.INSTANCE.toProto(messageSendConfiguration);
        }

        public static org.a2aproject.sdk.grpc.SendMessageRequest sendMessageRequest(MessageSendParams request) {
            return MessageSendParamsMapper.INSTANCE.toProto(request);
        }

        public static org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse listTaskPushNotificationConfigsResponse(ListTaskPushNotificationConfigsResult result) {
            List<org.a2aproject.sdk.grpc.TaskPushNotificationConfig> confs = new ArrayList<>(result.configs().size());
            for (TaskPushNotificationConfig config : result.configs()) {
                confs.add(taskPushNotificationConfig(config));
            }
            org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.Builder builder = org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.newBuilder().addAllConfigs(confs);
            if (result.nextPageToken() != null) {
                builder.setNextPageToken(result.nextPageToken());
            }
            return builder.build();
        }

        public static StreamResponse streamResponse(StreamingEventKind streamingEventKind) {
            return StreamResponseMapper.INSTANCE.toProto(streamingEventKind);
        }

        public static org.a2aproject.sdk.grpc.SendMessageResponse taskOrMessage(EventKind eventKind) {
            return switch (eventKind.kind()) {
                case Task.STREAMING_EVENT_ID -> org.a2aproject.sdk.grpc.SendMessageResponse.newBuilder()
                        .setTask(task((Task) eventKind))
                        .build();
                case Message.STREAMING_EVENT_ID -> org.a2aproject.sdk.grpc.SendMessageResponse.newBuilder()
                        .setMessage(message((Message) eventKind))
                        .build();
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            };
        }

        public static org.a2aproject.sdk.grpc.StreamResponse taskOrMessageStream(StreamingEventKind eventKind) {
            return switch (eventKind.kind()) {
                case Task.STREAMING_EVENT_ID -> org.a2aproject.sdk.grpc.StreamResponse.newBuilder()
                        .setTask(task((Task) eventKind))
                        .build();
                case Message.STREAMING_EVENT_ID -> org.a2aproject.sdk.grpc.StreamResponse.newBuilder()
                        .setMessage(message((Message) eventKind))
                        .build();
                case TaskStatusUpdateEvent.STREAMING_EVENT_ID -> org.a2aproject.sdk.grpc.StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent((TaskStatusUpdateEvent) eventKind))
                        .build();
                case TaskArtifactUpdateEvent.STREAMING_EVENT_ID -> org.a2aproject.sdk.grpc.StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent((TaskArtifactUpdateEvent) eventKind))
                        .build();
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            };
        }

        public static org.a2aproject.sdk.grpc.TaskPushNotificationConfig createTaskPushNotificationConfigResponse(TaskPushNotificationConfig config) {
            return taskPushNotificationConfig(config);
        }

        public static org.a2aproject.sdk.grpc.TaskPushNotificationConfig getTaskPushNotificationConfigResponse(TaskPushNotificationConfig config) {
            return taskPushNotificationConfig(config);
        }

        public static org.a2aproject.sdk.grpc.AgentCard getExtendedCardResponse(AgentCard card) {
            return agentCard(card);
        }
    }

    public static class FromProto {

        private static <T> T convert(java.util.function.Supplier<T> s) {
            try {
                return s.get();
            } catch (IllegalArgumentException ex) {
                throw new InvalidParamsError(ex.getMessage());
            }
        }

        public static AgentCard agentCard(org.a2aproject.sdk.grpc.AgentCardOrBuilder agentCard) {
            org.a2aproject.sdk.grpc.AgentCard agentCardProto = agentCard instanceof org.a2aproject.sdk.grpc.AgentCard
                    ? (org.a2aproject.sdk.grpc.AgentCard) agentCard
                    : ((org.a2aproject.sdk.grpc.AgentCard.Builder) agentCard).build();
            return convert(() -> AgentCardMapper.INSTANCE.fromProto(agentCardProto));
        }

        public static TaskQueryParams taskQueryParams(org.a2aproject.sdk.grpc.GetTaskRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.GetTaskRequest reqProto = request instanceof org.a2aproject.sdk.grpc.GetTaskRequest
                    ? (org.a2aproject.sdk.grpc.GetTaskRequest) request
                    : ((org.a2aproject.sdk.grpc.GetTaskRequest.Builder) request).build();
            return convert(() -> TaskQueryParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static ListTasksParams listTasksParams(org.a2aproject.sdk.grpc.ListTasksRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.ListTasksRequest reqProto = request instanceof org.a2aproject.sdk.grpc.ListTasksRequest
                    ? (org.a2aproject.sdk.grpc.ListTasksRequest) request
                    : ((org.a2aproject.sdk.grpc.ListTasksRequest.Builder) request).build();
            return convert(() -> ListTasksParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static CancelTaskParams cancelTaskParams(org.a2aproject.sdk.grpc.CancelTaskRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.CancelTaskRequest reqProto = request instanceof org.a2aproject.sdk.grpc.CancelTaskRequest
                    ? (org.a2aproject.sdk.grpc.CancelTaskRequest) request
                    : ((org.a2aproject.sdk.grpc.CancelTaskRequest.Builder) request).build();
            return convert(() -> TaskIdParamsMapper.INSTANCE.fromProtoCancelTaskRequest(reqProto));
        }

        public static MessageSendParams messageSendParams(org.a2aproject.sdk.grpc.SendMessageRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.SendMessageRequest requestProto = request instanceof org.a2aproject.sdk.grpc.SendMessageRequest
                    ? (org.a2aproject.sdk.grpc.SendMessageRequest) request
                    : ((org.a2aproject.sdk.grpc.SendMessageRequest.Builder) request).build();
            return convert(() -> MessageSendParamsMapper.INSTANCE.fromProto(requestProto));
        }

        public static TaskPushNotificationConfig createTaskPushNotificationConfig(org.a2aproject.sdk.grpc.TaskPushNotificationConfigOrBuilder config) {
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig proto = config instanceof org.a2aproject.sdk.grpc.TaskPushNotificationConfig
                    ? (org.a2aproject.sdk.grpc.TaskPushNotificationConfig) config
                    : ((org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder) config).build();
            return convert(() -> TaskPushNotificationConfigMapper.INSTANCE.fromProto(proto));
        }

        public static TaskPushNotificationConfig taskPushNotificationConfig(org.a2aproject.sdk.grpc.TaskPushNotificationConfigOrBuilder config) {
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig proto = config instanceof org.a2aproject.sdk.grpc.TaskPushNotificationConfig
                    ? (org.a2aproject.sdk.grpc.TaskPushNotificationConfig) config
                    : ((org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder) config).build();
            return convert(() -> TaskPushNotificationConfigMapper.INSTANCE.fromProto(proto));
        }

        public static GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams(org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest reqProto = request instanceof org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest
                    ? (org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest) request
                    : ((org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.Builder) request).build();
            return convert(() -> GetTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static TaskIdParams taskIdParams(org.a2aproject.sdk.grpc.SubscribeToTaskRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.SubscribeToTaskRequest reqProto = request instanceof org.a2aproject.sdk.grpc.SubscribeToTaskRequest
                    ? (org.a2aproject.sdk.grpc.SubscribeToTaskRequest) request
                    : ((org.a2aproject.sdk.grpc.SubscribeToTaskRequest.Builder) request).build();
            return convert(() -> TaskIdParamsMapper.INSTANCE.fromProtoSubscribeToTaskRequest(reqProto));
        }

        public static ListTaskPushNotificationConfigsResult listTaskPushNotificationConfigsResult(org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponseOrBuilder response) {
            List<org.a2aproject.sdk.grpc.TaskPushNotificationConfig> configs = response.getConfigsList();
            List<TaskPushNotificationConfig> result = new ArrayList<>(configs.size());
            for (org.a2aproject.sdk.grpc.TaskPushNotificationConfig config : configs) {
                result.add(taskPushNotificationConfig(config));
            }
            String nextPageToken = response.getNextPageToken();
            if (nextPageToken != null && nextPageToken.isEmpty()) {
                nextPageToken = null;
            }
            return new ListTaskPushNotificationConfigsResult(result, nextPageToken);
        }

        public static ListTaskPushNotificationConfigsParams listTaskPushNotificationConfigsParams(org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest reqProto = request instanceof org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest
                    ? (org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest) request
                    : ((org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.Builder) request).build();
            return convert(() -> ListTaskPushNotificationConfigsParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams(org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder request) {
            org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest reqProto = request instanceof org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest
                    ? (org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest) request
                    : ((org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.Builder) request).build();
            return convert(() -> DeleteTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static Task task(org.a2aproject.sdk.grpc.TaskOrBuilder task) {
            org.a2aproject.sdk.grpc.Task taskProto = task instanceof org.a2aproject.sdk.grpc.Task
                    ? (org.a2aproject.sdk.grpc.Task) task
                    : ((org.a2aproject.sdk.grpc.Task.Builder) task).build();
            return convert(() -> TaskMapper.INSTANCE.fromProto(taskProto));
        }

        public static Message message(org.a2aproject.sdk.grpc.MessageOrBuilder message) {
            if (message.getMessageId().isEmpty()) {
                throw new InvalidParamsError();
            }
            org.a2aproject.sdk.grpc.Message messageProto = message instanceof org.a2aproject.sdk.grpc.Message
                    ? (org.a2aproject.sdk.grpc.Message) message
                    : ((org.a2aproject.sdk.grpc.Message.Builder) message).build();
            return convert(() -> MessageMapper.INSTANCE.fromProto(messageProto));
        }

        public static TaskStatusUpdateEvent taskStatusUpdateEvent(org.a2aproject.sdk.grpc.TaskStatusUpdateEventOrBuilder taskStatusUpdateEvent) {
            org.a2aproject.sdk.grpc.TaskStatusUpdateEvent eventProto = taskStatusUpdateEvent instanceof org.a2aproject.sdk.grpc.TaskStatusUpdateEvent
                    ? (org.a2aproject.sdk.grpc.TaskStatusUpdateEvent) taskStatusUpdateEvent
                    : ((org.a2aproject.sdk.grpc.TaskStatusUpdateEvent.Builder) taskStatusUpdateEvent).build();
            return convert(() -> TaskStatusUpdateEventMapper.INSTANCE.fromProto(eventProto));
        }

        public static TaskArtifactUpdateEvent taskArtifactUpdateEvent(org.a2aproject.sdk.grpc.TaskArtifactUpdateEventOrBuilder taskArtifactUpdateEvent) {
            org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent eventProto = taskArtifactUpdateEvent instanceof org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent
                    ? (org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent) taskArtifactUpdateEvent
                    : ((org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent.Builder) taskArtifactUpdateEvent).build();
            return convert(() -> TaskArtifactUpdateEventMapper.INSTANCE.fromProto(eventProto));
        }

        public static ListTasksResult listTasksResult(org.a2aproject.sdk.grpc.ListTasksResponseOrBuilder listTasksResponse) {
            org.a2aproject.sdk.grpc.ListTasksResponse eventProto = listTasksResponse instanceof org.a2aproject.sdk.grpc.ListTasksResponse
                    ? (org.a2aproject.sdk.grpc.ListTasksResponse) listTasksResponse
                    : ((org.a2aproject.sdk.grpc.ListTasksResponse.Builder) listTasksResponse).build();
            return convert(() -> ListTasksResultMapper.INSTANCE.fromProto(eventProto));
        }

        public static StreamingEventKind streamingEventKind(org.a2aproject.sdk.grpc.StreamResponseOrBuilder streamResponse) {
            org.a2aproject.sdk.grpc.StreamResponse streamResponseProto = streamResponse instanceof org.a2aproject.sdk.grpc.StreamResponse
                    ? (org.a2aproject.sdk.grpc.StreamResponse) streamResponse
                    : ((org.a2aproject.sdk.grpc.StreamResponse.Builder) streamResponse).build();
            return convert(() -> StreamResponseMapper.INSTANCE.fromProto(streamResponseProto));
        }
    }

}
