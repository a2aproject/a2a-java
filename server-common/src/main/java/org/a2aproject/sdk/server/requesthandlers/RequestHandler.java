package org.a2aproject.sdk.server.requesthandlers;

import java.util.concurrent.Flow;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.jspecify.annotations.Nullable;

public interface RequestHandler {
    Task onGetTask(
            TaskQueryParams params,
            ServerCallContext context) throws A2AError;

    ListTasksResult onListTasks(
            ListTasksParams params,
            ServerCallContext context) throws A2AError;

    Task onCancelTask(
            CancelTaskParams params,
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

    ListTaskPushNotificationConfigsResult onListTaskPushNotificationConfigs(
            ListTaskPushNotificationConfigsParams params,
            ServerCallContext context) throws A2AError;

    void onDeleteTaskPushNotificationConfig(
            DeleteTaskPushNotificationConfigParams params,
            ServerCallContext context) throws A2AError;

    void validateRequestedTask(@Nullable String requestedTaskId) throws A2AError;
}
