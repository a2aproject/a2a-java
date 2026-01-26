package io.a2a.grpc.utils;

import java.util.ArrayList;
import java.util.List;

import io.a2a.grpc.GetExtendedAgentCardRequest;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.mapper.AgentCardMapper;
import io.a2a.grpc.mapper.DeleteTaskPushNotificationConfigParamsMapper;
import io.a2a.grpc.mapper.GetTaskPushNotificationConfigParamsMapper;
import io.a2a.grpc.mapper.ListTaskPushNotificationConfigParamsMapper;
import io.a2a.grpc.mapper.ListTasksParamsMapper;
import io.a2a.grpc.mapper.ListTasksResultMapper;
import io.a2a.grpc.mapper.MessageMapper;
import io.a2a.grpc.mapper.MessageSendConfigurationMapper;
import io.a2a.grpc.mapper.MessageSendParamsMapper;
import io.a2a.grpc.mapper.CreateTaskPushNotificationConfigMapper;
import io.a2a.grpc.mapper.StreamResponseMapper;
import io.a2a.grpc.mapper.TaskArtifactUpdateEventMapper;
import io.a2a.grpc.mapper.TaskIdParamsMapper;
import io.a2a.grpc.mapper.TaskMapper;
import io.a2a.grpc.mapper.TaskPushNotificationConfigMapper;
import io.a2a.grpc.mapper.TaskQueryParamsMapper;
import io.a2a.grpc.mapper.TaskStateMapper;
import io.a2a.grpc.mapper.TaskStatusUpdateEventMapper;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;

/**
 * Utility class to convert between GRPC and Spec objects.
 */
public class ProtoUtils {

    public static class ToProto {

        public static io.a2a.grpc.AgentCard agentCard(AgentCard agentCard) {
            return AgentCardMapper.INSTANCE.toProto(agentCard);
        }

        public static io.a2a.grpc.GetExtendedAgentCardRequest extendedAgentCard() {
            return GetExtendedAgentCardRequest.newBuilder().build();
        }

        public static io.a2a.grpc.GetTaskRequest getTaskRequest(TaskQueryParams params) {
            return TaskQueryParamsMapper.INSTANCE.toProto(params);
        }

        public static io.a2a.grpc.CancelTaskRequest cancelTaskRequest(TaskIdParams params) {
            return TaskIdParamsMapper.INSTANCE.toProtoCancelTaskRequest(params);
        }

        public static io.a2a.grpc.SubscribeToTaskRequest subscribeToTaskRequest(TaskIdParams params) {
            return TaskIdParamsMapper.INSTANCE.toProtoSubscribeToTaskRequest(params);
        }

        public static io.a2a.grpc.CreateTaskPushNotificationConfigRequest createTaskPushNotificationConfigRequest(TaskPushNotificationConfig config) {
            return CreateTaskPushNotificationConfigMapper.INSTANCE.toProto(config);
        }

        public static io.a2a.grpc.GetTaskPushNotificationConfigRequest getTaskPushNotificationConfigRequest(GetTaskPushNotificationConfigParams params) {
            return GetTaskPushNotificationConfigParamsMapper.INSTANCE.toProto(params);
        }

        public static io.a2a.grpc.DeleteTaskPushNotificationConfigRequest deleteTaskPushNotificationConfigRequest(DeleteTaskPushNotificationConfigParams params) {
            return DeleteTaskPushNotificationConfigParamsMapper.INSTANCE.toProto(params);
        }

        public static io.a2a.grpc.ListTaskPushNotificationConfigRequest listTaskPushNotificationConfigRequest(ListTaskPushNotificationConfigParams params) {
            return ListTaskPushNotificationConfigParamsMapper.INSTANCE.toProto(params);
        }

        public static io.a2a.grpc.Task task(Task task) {
            return TaskMapper.INSTANCE.toProto(task);
        }

        public static io.a2a.grpc.ListTasksResponse listTasksResult(ListTasksResult result) {
            return ListTasksResultMapper.INSTANCE.toProto(result);
        }

        public static io.a2a.grpc.ListTasksRequest listTasksParams(ListTasksParams params) {
            return ListTasksParamsMapper.INSTANCE.toProto(params);
        }

        public static io.a2a.grpc.Message message(Message message) {
            return MessageMapper.INSTANCE.toProto(message);
        }

        public static io.a2a.grpc.TaskPushNotificationConfig taskPushNotificationConfig(TaskPushNotificationConfig config) {
            return TaskPushNotificationConfigMapper.INSTANCE.toProto(config);
        }

        public static io.a2a.grpc.TaskArtifactUpdateEvent taskArtifactUpdateEvent(TaskArtifactUpdateEvent event) {
            return TaskArtifactUpdateEventMapper.INSTANCE.toProto(event);
        }

        public static io.a2a.grpc.TaskStatusUpdateEvent taskStatusUpdateEvent(TaskStatusUpdateEvent event) {
            return TaskStatusUpdateEventMapper.INSTANCE.toProto(event);
        }

        public static io.a2a.grpc.TaskState taskState(TaskState taskState) {
            return TaskStateMapper.INSTANCE.toProto(taskState);
        }

        public static io.a2a.grpc.SendMessageConfiguration messageSendConfiguration(MessageSendConfiguration messageSendConfiguration) {
            return MessageSendConfigurationMapper.INSTANCE.toProto(messageSendConfiguration);
        }

        public static io.a2a.grpc.SendMessageRequest sendMessageRequest(MessageSendParams request) {
            return MessageSendParamsMapper.INSTANCE.toProto(request);
        }

        public static io.a2a.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfigResponse(ListTaskPushNotificationConfigResult result) {
            List<io.a2a.grpc.TaskPushNotificationConfig> confs = new ArrayList<>(result.configs().size());
            for (TaskPushNotificationConfig config : result.configs()) {
                confs.add(taskPushNotificationConfig(config));
            }
            io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder builder = io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder().addAllConfigs(confs);
            if (result.nextPageToken() != null) {
                builder.setNextPageToken(result.nextPageToken());
            }
            return builder.build();
        }

        public static StreamResponse streamResponse(StreamingEventKind streamingEventKind) {
            return StreamResponseMapper.INSTANCE.toProto(streamingEventKind);
        }

        public static io.a2a.grpc.SendMessageResponse taskOrMessage(EventKind eventKind) {
            return switch (eventKind.kind()) {
                case Task.STREAMING_EVENT_ID -> io.a2a.grpc.SendMessageResponse.newBuilder()
                        .setTask(task((Task) eventKind))
                        .build();
                case Message.STREAMING_EVENT_ID -> io.a2a.grpc.SendMessageResponse.newBuilder()
                        .setMessage(message((Message) eventKind))
                        .build();
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            };
        }

        public static io.a2a.grpc.StreamResponse taskOrMessageStream(StreamingEventKind eventKind) {
            return switch (eventKind.kind()) {
                case Task.STREAMING_EVENT_ID -> io.a2a.grpc.StreamResponse.newBuilder()
                        .setTask(task((Task) eventKind))
                        .build();
                case Message.STREAMING_EVENT_ID -> io.a2a.grpc.StreamResponse.newBuilder()
                        .setMessage(message((Message) eventKind))
                        .build();
                case TaskStatusUpdateEvent.STREAMING_EVENT_ID -> io.a2a.grpc.StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent((TaskStatusUpdateEvent) eventKind))
                        .build();
                case TaskArtifactUpdateEvent.STREAMING_EVENT_ID -> io.a2a.grpc.StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent((TaskArtifactUpdateEvent) eventKind))
                        .build();
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            };
        }

        public static io.a2a.grpc.TaskPushNotificationConfig createTaskPushNotificationConfigResponse(TaskPushNotificationConfig config) {
            return taskPushNotificationConfig(config);
        }

        public static io.a2a.grpc.TaskPushNotificationConfig getTaskPushNotificationConfigResponse(TaskPushNotificationConfig config) {
            return taskPushNotificationConfig(config);
        }

        public static io.a2a.grpc.AgentCard getExtendedCardResponse(AgentCard card) {
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

        public static AgentCard agentCard(io.a2a.grpc.AgentCardOrBuilder agentCard) {
            io.a2a.grpc.AgentCard agentCardProto = agentCard instanceof io.a2a.grpc.AgentCard
                    ? (io.a2a.grpc.AgentCard) agentCard
                    : ((io.a2a.grpc.AgentCard.Builder) agentCard).build();
            return convert(() -> AgentCardMapper.INSTANCE.fromProto(agentCardProto));
        }

        public static TaskQueryParams taskQueryParams(io.a2a.grpc.GetTaskRequestOrBuilder request) {
            io.a2a.grpc.GetTaskRequest reqProto = request instanceof io.a2a.grpc.GetTaskRequest
                    ? (io.a2a.grpc.GetTaskRequest) request
                    : ((io.a2a.grpc.GetTaskRequest.Builder) request).build();
            return convert(() -> TaskQueryParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static ListTasksParams listTasksParams(io.a2a.grpc.ListTasksRequestOrBuilder request) {
            io.a2a.grpc.ListTasksRequest reqProto = request instanceof io.a2a.grpc.ListTasksRequest
                    ? (io.a2a.grpc.ListTasksRequest) request
                    : ((io.a2a.grpc.ListTasksRequest.Builder) request).build();
            return convert(() -> ListTasksParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static TaskIdParams taskIdParams(io.a2a.grpc.CancelTaskRequestOrBuilder request) {
            io.a2a.grpc.CancelTaskRequest reqProto = request instanceof io.a2a.grpc.CancelTaskRequest
                    ? (io.a2a.grpc.CancelTaskRequest) request
                    : ((io.a2a.grpc.CancelTaskRequest.Builder) request).build();
            return convert(() -> TaskIdParamsMapper.INSTANCE.fromProtoCancelTaskRequest(reqProto));
        }

        public static MessageSendParams messageSendParams(io.a2a.grpc.SendMessageRequestOrBuilder request) {
            io.a2a.grpc.SendMessageRequest requestProto = request instanceof io.a2a.grpc.SendMessageRequest
                    ? (io.a2a.grpc.SendMessageRequest) request
                    : ((io.a2a.grpc.SendMessageRequest.Builder) request).build();
            return convert(() -> MessageSendParamsMapper.INSTANCE.fromProto(requestProto));
        }

        public static TaskPushNotificationConfig CreateTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequestOrBuilder config) {
            io.a2a.grpc.CreateTaskPushNotificationConfigRequest reqProto = config instanceof io.a2a.grpc.CreateTaskPushNotificationConfigRequest
                    ? (io.a2a.grpc.CreateTaskPushNotificationConfigRequest) config
                    : ((io.a2a.grpc.CreateTaskPushNotificationConfigRequest.Builder) config).build();
            return convert(() -> CreateTaskPushNotificationConfigMapper.INSTANCE.fromProto(reqProto));
        }

        public static TaskPushNotificationConfig taskPushNotificationConfig(io.a2a.grpc.TaskPushNotificationConfigOrBuilder config) {
            io.a2a.grpc.TaskPushNotificationConfig proto = config instanceof io.a2a.grpc.TaskPushNotificationConfig
                    ? (io.a2a.grpc.TaskPushNotificationConfig) config
                    : ((io.a2a.grpc.TaskPushNotificationConfig.Builder) config).build();
            return convert(() -> TaskPushNotificationConfigMapper.INSTANCE.fromProto(proto));
        }

        public static GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams(io.a2a.grpc.GetTaskPushNotificationConfigRequestOrBuilder request) {
            io.a2a.grpc.GetTaskPushNotificationConfigRequest reqProto = request instanceof io.a2a.grpc.GetTaskPushNotificationConfigRequest
                    ? (io.a2a.grpc.GetTaskPushNotificationConfigRequest) request
                    : ((io.a2a.grpc.GetTaskPushNotificationConfigRequest.Builder) request).build();
            return convert(() -> GetTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static TaskIdParams taskIdParams(io.a2a.grpc.SubscribeToTaskRequestOrBuilder request) {
            io.a2a.grpc.SubscribeToTaskRequest reqProto = request instanceof io.a2a.grpc.SubscribeToTaskRequest
                    ? (io.a2a.grpc.SubscribeToTaskRequest) request
                    : ((io.a2a.grpc.SubscribeToTaskRequest.Builder) request).build();
            return convert(() -> TaskIdParamsMapper.INSTANCE.fromProtoSubscribeToTaskRequest(reqProto));
        }

        public static ListTaskPushNotificationConfigResult listTaskPushNotificationConfigResult(io.a2a.grpc.ListTaskPushNotificationConfigResponseOrBuilder response) {
            List<io.a2a.grpc.TaskPushNotificationConfig> configs = response.getConfigsList();
            List<TaskPushNotificationConfig> result = new ArrayList<>(configs.size());
            for (io.a2a.grpc.TaskPushNotificationConfig config : configs) {
                result.add(taskPushNotificationConfig(config));
            }
            String nextPageToken = response.getNextPageToken();
            if (nextPageToken != null && nextPageToken.isEmpty()) {
                nextPageToken = null;
            }
            return new ListTaskPushNotificationConfigResult(result, nextPageToken);
        }

        public static ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams(io.a2a.grpc.ListTaskPushNotificationConfigRequestOrBuilder request) {
            io.a2a.grpc.ListTaskPushNotificationConfigRequest reqProto = request instanceof io.a2a.grpc.ListTaskPushNotificationConfigRequest
                    ? (io.a2a.grpc.ListTaskPushNotificationConfigRequest) request
                    : ((io.a2a.grpc.ListTaskPushNotificationConfigRequest.Builder) request).build();
            return convert(() -> ListTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams(io.a2a.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder request) {
            io.a2a.grpc.DeleteTaskPushNotificationConfigRequest reqProto = request instanceof io.a2a.grpc.DeleteTaskPushNotificationConfigRequest
                    ? (io.a2a.grpc.DeleteTaskPushNotificationConfigRequest) request
                    : ((io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.Builder) request).build();
            return convert(() -> DeleteTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto));
        }

        public static Task task(io.a2a.grpc.TaskOrBuilder task) {
            io.a2a.grpc.Task taskProto = task instanceof io.a2a.grpc.Task
                    ? (io.a2a.grpc.Task) task
                    : ((io.a2a.grpc.Task.Builder) task).build();
            return convert(() -> TaskMapper.INSTANCE.fromProto(taskProto));
        }

        public static Message message(io.a2a.grpc.MessageOrBuilder message) {
            if (message.getMessageId().isEmpty()) {
                throw new InvalidParamsError();
            }
            io.a2a.grpc.Message messageProto = message instanceof io.a2a.grpc.Message
                    ? (io.a2a.grpc.Message) message
                    : ((io.a2a.grpc.Message.Builder) message).build();
            return convert(() -> MessageMapper.INSTANCE.fromProto(messageProto));
        }

        public static TaskStatusUpdateEvent taskStatusUpdateEvent(io.a2a.grpc.TaskStatusUpdateEventOrBuilder taskStatusUpdateEvent) {
            io.a2a.grpc.TaskStatusUpdateEvent eventProto = taskStatusUpdateEvent instanceof io.a2a.grpc.TaskStatusUpdateEvent
                    ? (io.a2a.grpc.TaskStatusUpdateEvent) taskStatusUpdateEvent
                    : ((io.a2a.grpc.TaskStatusUpdateEvent.Builder) taskStatusUpdateEvent).build();
            return convert(() -> TaskStatusUpdateEventMapper.INSTANCE.fromProto(eventProto));
        }

        public static TaskArtifactUpdateEvent taskArtifactUpdateEvent(io.a2a.grpc.TaskArtifactUpdateEventOrBuilder taskArtifactUpdateEvent) {
            io.a2a.grpc.TaskArtifactUpdateEvent eventProto = taskArtifactUpdateEvent instanceof io.a2a.grpc.TaskArtifactUpdateEvent
                    ? (io.a2a.grpc.TaskArtifactUpdateEvent) taskArtifactUpdateEvent
                    : ((io.a2a.grpc.TaskArtifactUpdateEvent.Builder) taskArtifactUpdateEvent).build();
            return convert(() -> TaskArtifactUpdateEventMapper.INSTANCE.fromProto(eventProto));
        }

        public static ListTasksResult listTasksResult(io.a2a.grpc.ListTasksResponseOrBuilder listTasksResponse) {
            io.a2a.grpc.ListTasksResponse eventProto = listTasksResponse instanceof io.a2a.grpc.ListTasksResponse
                    ? (io.a2a.grpc.ListTasksResponse) listTasksResponse
                    : ((io.a2a.grpc.ListTasksResponse.Builder) listTasksResponse).build();
            return convert(() -> ListTasksResultMapper.INSTANCE.fromProto(eventProto));
        }

        public static StreamingEventKind streamingEventKind(io.a2a.grpc.StreamResponseOrBuilder streamResponse) {
            io.a2a.grpc.StreamResponse streamResponseProto = streamResponse instanceof io.a2a.grpc.StreamResponse
                    ? (io.a2a.grpc.StreamResponse) streamResponse
                    : ((io.a2a.grpc.StreamResponse.Builder) streamResponse).build();
            return convert(() -> StreamResponseMapper.INSTANCE.fromProto(streamResponseProto));
        }
    }

}
