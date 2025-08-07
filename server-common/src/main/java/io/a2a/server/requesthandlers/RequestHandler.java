package io.a2a.server.requesthandlers;

import java.util.List;
import java.util.concurrent.Flow;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;

public interface RequestHandler {
    Task onGetTask(
            TaskQueryParams params,
            ServerCallContext context) throws JSONRPCError;

    Task onCancelTask(
            TaskIdParams params,
            ServerCallContext context) throws JSONRPCError;

    EventKind onMessageSend(
            MessageSendParams params,
            ServerCallContext context) throws JSONRPCError;

    Flow.Publisher<StreamingEventKind> onMessageSendStream(
            MessageSendParams params,
            ServerCallContext context) throws JSONRPCError;

    TaskPushNotificationConfig onSetTaskPushNotificationConfig(
            TaskPushNotificationConfig params,
            ServerCallContext context) throws JSONRPCError;

    TaskPushNotificationConfig onGetTaskPushNotificationConfig(
            GetTaskPushNotificationConfigParams params,
            ServerCallContext context) throws JSONRPCError;

    Flow.Publisher<StreamingEventKind> onResubscribeToTask(
            TaskIdParams params,
            ServerCallContext context) throws JSONRPCError;

    List<TaskPushNotificationConfig> onListTaskPushNotificationConfig(
            ListTaskPushNotificationConfigParams params,
            ServerCallContext context) throws JSONRPCError;

    void onDeleteTaskPushNotificationConfig(
            DeleteTaskPushNotificationConfigParams params,
            ServerCallContext context) throws JSONRPCError;
}
