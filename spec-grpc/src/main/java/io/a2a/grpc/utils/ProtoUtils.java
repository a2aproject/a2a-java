package io.a2a.grpc.utils;


import java.util.ArrayList;
import java.util.List;

import io.a2a.grpc.ListTasksRequest;
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
import io.a2a.grpc.mapper.SetTaskPushNotificationConfigMapper;
import io.a2a.grpc.mapper.TaskArtifactUpdateEventMapper;
import io.a2a.grpc.mapper.TaskIdParamsMapper;
import io.a2a.grpc.mapper.TaskMapper;
import io.a2a.grpc.mapper.TaskPushNotificationConfigMapper;
import io.a2a.grpc.mapper.TaskQueryParamsMapper;
import io.a2a.grpc.mapper.TaskStateMapper;
import io.a2a.grpc.mapper.TaskStatusUpdateEventMapper;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
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

        public static io.a2a.grpc.Task task(Task task) {
            return TaskMapper.INSTANCE.toProto(task);
        }

        public static io.a2a.grpc.ListTasksResponse listTasksResult(io.a2a.spec.ListTasksResult result) {
            return ListTasksResultMapper.INSTANCE.toProto(result);
        }

        public static io.a2a.grpc.ListTasksRequest listTasksParams(io.a2a.spec.ListTasksParams params) {
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

        public static io.a2a.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfigResponse(List<TaskPushNotificationConfig> configs) {
            List<io.a2a.grpc.TaskPushNotificationConfig> confs = new ArrayList<>(configs.size());
            for(TaskPushNotificationConfig config: configs) {
                confs.add(taskPushNotificationConfig(config));
            }
            return io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder().addAllConfigs(confs).build();
        }

        public static StreamResponse streamResponse(StreamingEventKind streamingEventKind) {
            // This makes a decision and wraps the MapStruct Mappers
            if (streamingEventKind instanceof TaskStatusUpdateEvent) {
                return StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent((TaskStatusUpdateEvent) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof TaskArtifactUpdateEvent) {
                return StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent((TaskArtifactUpdateEvent) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof Message) {
                return StreamResponse.newBuilder()
                        .setMsg(message((Message) streamingEventKind))
                        .build();
            } else if (streamingEventKind instanceof Task) {
                return StreamResponse.newBuilder()
                        .setTask(task((Task) streamingEventKind))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + streamingEventKind);
            }
        }

        public static io.a2a.grpc.SendMessageResponse taskOrMessage(EventKind eventKind) {
            if (eventKind instanceof Task) {
                return io.a2a.grpc.SendMessageResponse.newBuilder()
                        .setTask(task((Task) eventKind))
                        .build();
            } else if (eventKind instanceof Message) {
                return io.a2a.grpc.SendMessageResponse.newBuilder()
                        .setMsg(message((Message) eventKind))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            }
        }

        public static io.a2a.grpc.StreamResponse taskOrMessageStream(StreamingEventKind eventKind) {
            if (eventKind instanceof Task task) {
                return io.a2a.grpc.StreamResponse.newBuilder()
                        .setTask(task(task))
                        .build();
            } else if (eventKind instanceof Message msg) {
                return io.a2a.grpc.StreamResponse.newBuilder()
                        .setMsg(message(msg))
                        .build();
            } else if (eventKind instanceof TaskArtifactUpdateEvent update) {
                return io.a2a.grpc.StreamResponse.newBuilder()
                        .setArtifactUpdate(taskArtifactUpdateEvent(update))
                        .build();
            } else if (eventKind instanceof TaskStatusUpdateEvent update) {
                return io.a2a.grpc.StreamResponse.newBuilder()
                        .setStatusUpdate(taskStatusUpdateEvent(update))
                        .build();
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + eventKind);
            }
        }

    }

    public static class FromProto {

        public static TaskQueryParams taskQueryParams(io.a2a.grpc.GetTaskRequestOrBuilder request) {
            io.a2a.grpc.GetTaskRequest reqProto = request instanceof io.a2a.grpc.GetTaskRequest
                ? (io.a2a.grpc.GetTaskRequest) request
                : ((io.a2a.grpc.GetTaskRequest.Builder) request).build();
            return TaskQueryParamsMapper.INSTANCE.fromProto(reqProto);
        }

        public static io.a2a.spec.ListTasksParams listTasksParams(io.a2a.grpc.ListTasksRequestOrBuilder request) {
            ListTasksRequest reqProto = request instanceof ListTasksRequest
                ? (ListTasksRequest) request
                : ((ListTasksRequest.Builder) request).build();
            return ListTasksParamsMapper.INSTANCE.fromProto(reqProto);
        }

        public static TaskIdParams taskIdParams(io.a2a.grpc.CancelTaskRequestOrBuilder request) {
            io.a2a.grpc.CancelTaskRequest reqProto = request instanceof io.a2a.grpc.CancelTaskRequest
                ? (io.a2a.grpc.CancelTaskRequest) request
                : ((io.a2a.grpc.CancelTaskRequest.Builder) request).build();
            return TaskIdParamsMapper.INSTANCE.fromProtoCancelTaskRequest(reqProto);
        }

        public static MessageSendParams messageSendParams(io.a2a.grpc.SendMessageRequestOrBuilder request) {
            io.a2a.grpc.SendMessageRequest requestProto = request instanceof io.a2a.grpc.SendMessageRequest
                ? (io.a2a.grpc.SendMessageRequest) request
                : ((io.a2a.grpc.SendMessageRequest.Builder) request).build();
            return MessageSendParamsMapper.INSTANCE.fromProto(requestProto);
        }

        public static TaskPushNotificationConfig setTaskPushNotificationConfig(io.a2a.grpc.SetTaskPushNotificationConfigRequestOrBuilder config) {
            io.a2a.grpc.SetTaskPushNotificationConfigRequest reqProto = config instanceof io.a2a.grpc.SetTaskPushNotificationConfigRequest
                ? (io.a2a.grpc.SetTaskPushNotificationConfigRequest) config
                : ((io.a2a.grpc.SetTaskPushNotificationConfigRequest.Builder) config).build();
            return SetTaskPushNotificationConfigMapper.INSTANCE.fromProto(reqProto);
        }

        public static TaskPushNotificationConfig taskPushNotificationConfig(io.a2a.grpc.TaskPushNotificationConfigOrBuilder config) {
            io.a2a.grpc.TaskPushNotificationConfig proto = config instanceof io.a2a.grpc.TaskPushNotificationConfig
                ? (io.a2a.grpc.TaskPushNotificationConfig) config
                : ((io.a2a.grpc.TaskPushNotificationConfig.Builder) config).build();
            return TaskPushNotificationConfigMapper.INSTANCE.fromProto(proto);
        }

        public static GetTaskPushNotificationConfigParams getTaskPushNotificationConfigParams(io.a2a.grpc.GetTaskPushNotificationConfigRequestOrBuilder request) {
            io.a2a.grpc.GetTaskPushNotificationConfigRequest reqProto = request instanceof io.a2a.grpc.GetTaskPushNotificationConfigRequest
                ? (io.a2a.grpc.GetTaskPushNotificationConfigRequest) request
                : ((io.a2a.grpc.GetTaskPushNotificationConfigRequest.Builder) request).build();
            return GetTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto);
        }

        public static TaskIdParams taskIdParams(io.a2a.grpc.SubscribeToTaskRequestOrBuilder request) {
            io.a2a.grpc.SubscribeToTaskRequest reqProto = request instanceof io.a2a.grpc.SubscribeToTaskRequest
                ? (io.a2a.grpc.SubscribeToTaskRequest) request
                : ((io.a2a.grpc.SubscribeToTaskRequest.Builder) request).build();
            return TaskIdParamsMapper.INSTANCE.fromProtoSubscribeToTaskRequest(reqProto);
        }

        public static List<TaskPushNotificationConfig> listTaskPushNotificationConfigParams(io.a2a.grpc.ListTaskPushNotificationConfigResponseOrBuilder response) {
            List<io.a2a.grpc.TaskPushNotificationConfig> configs = response.getConfigsList();
            List<TaskPushNotificationConfig> result = new ArrayList<>(configs.size());
            for(io.a2a.grpc.TaskPushNotificationConfig config : configs) {
                result.add(taskPushNotificationConfig(config));
            }
            return result;
        }

        public static ListTaskPushNotificationConfigParams listTaskPushNotificationConfigParams(io.a2a.grpc.ListTaskPushNotificationConfigRequestOrBuilder request) {
            io.a2a.grpc.ListTaskPushNotificationConfigRequest reqProto = request instanceof io.a2a.grpc.ListTaskPushNotificationConfigRequest
                ? (io.a2a.grpc.ListTaskPushNotificationConfigRequest) request
                : ((io.a2a.grpc.ListTaskPushNotificationConfigRequest.Builder) request).build();
            return ListTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto);
        }

        public static DeleteTaskPushNotificationConfigParams deleteTaskPushNotificationConfigParams(io.a2a.grpc.DeleteTaskPushNotificationConfigRequestOrBuilder request) {
            io.a2a.grpc.DeleteTaskPushNotificationConfigRequest reqProto = request instanceof io.a2a.grpc.DeleteTaskPushNotificationConfigRequest
                ? (io.a2a.grpc.DeleteTaskPushNotificationConfigRequest) request
                : ((io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.Builder) request).build();
            return DeleteTaskPushNotificationConfigParamsMapper.INSTANCE.fromProto(reqProto);
        }

        public static Task task(io.a2a.grpc.TaskOrBuilder task) {
            io.a2a.grpc.Task taskProto = task instanceof io.a2a.grpc.Task
                ? (io.a2a.grpc.Task) task
                : ((io.a2a.grpc.Task.Builder) task).build();
            return TaskMapper.INSTANCE.fromProto(taskProto);
        }

        public static Message message(io.a2a.grpc.MessageOrBuilder message) {
            if (message.getMessageId().isEmpty()) {
                throw new InvalidParamsError();
            }
            io.a2a.grpc.Message messageProto = message instanceof io.a2a.grpc.Message
                ? (io.a2a.grpc.Message) message
                : ((io.a2a.grpc.Message.Builder) message).build();
            return MessageMapper.INSTANCE.fromProto(messageProto);
        }

        public static TaskStatusUpdateEvent taskStatusUpdateEvent(io.a2a.grpc.TaskStatusUpdateEventOrBuilder taskStatusUpdateEvent) {
            io.a2a.grpc.TaskStatusUpdateEvent eventProto = taskStatusUpdateEvent instanceof io.a2a.grpc.TaskStatusUpdateEvent
                ? (io.a2a.grpc.TaskStatusUpdateEvent) taskStatusUpdateEvent
                : ((io.a2a.grpc.TaskStatusUpdateEvent.Builder) taskStatusUpdateEvent).build();
            return TaskStatusUpdateEventMapper.INSTANCE.fromProto(eventProto);
        }

        public static TaskArtifactUpdateEvent taskArtifactUpdateEvent(io.a2a.grpc.TaskArtifactUpdateEventOrBuilder taskArtifactUpdateEvent) {
            io.a2a.grpc.TaskArtifactUpdateEvent eventProto = taskArtifactUpdateEvent instanceof io.a2a.grpc.TaskArtifactUpdateEvent
                ? (io.a2a.grpc.TaskArtifactUpdateEvent) taskArtifactUpdateEvent
                : ((io.a2a.grpc.TaskArtifactUpdateEvent.Builder) taskArtifactUpdateEvent).build();
            return TaskArtifactUpdateEventMapper.INSTANCE.fromProto(eventProto);
        }
    }

}
