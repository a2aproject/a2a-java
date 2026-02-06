package io.a2a.server.requesthandlers;

import java.util.concurrent.Flow;

import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.server.ServerCallContext;
import io.a2a.spec.A2AError;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;

public interface RequestHandler {
    Task onGetTask(
            TaskQueryParams params,
            ServerCallContext context) throws A2AError;

    ListTasksResult onListTasks(
            ListTasksParams params,
            ServerCallContext context) throws A2AError;

    Task onCancelTask(
            TaskIdParams params,
            ServerCallContext context) throws A2AError;

    EventKind onMessageSend(
            MessageSendParams params,
            ServerCallContext context) throws A2AError;

    Flow.Publisher<StreamingEventKind> onMessageSendStream(
            MessageSendParams params,
            ServerCallContext context) throws A2AError;

    TaskPushNotificationConfig onCreateTaskPushNotificationConfig(
            TaskPushNotificationConfig params,
            ServerCallContext context) throws A2AError;

    TaskPushNotificationConfig onGetTaskPushNotificationConfig(
            GetTaskPushNotificationConfigParams params,
            ServerCallContext context) throws A2AError;

    Flow.Publisher<StreamingEventKind> onSubscribeToTask(
            TaskIdParams params,
            ServerCallContext context) throws A2AError;

    ListTaskPushNotificationConfigResult onListTaskPushNotificationConfig(
            ListTaskPushNotificationConfigParams params,
            ServerCallContext context) throws A2AError;

    void onDeleteTaskPushNotificationConfig(
            DeleteTaskPushNotificationConfigParams params,
            ServerCallContext context) throws A2AError;
}
