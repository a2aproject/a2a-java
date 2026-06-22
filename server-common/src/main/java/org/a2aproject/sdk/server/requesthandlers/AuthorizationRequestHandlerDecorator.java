package org.a2aproject.sdk.server.requesthandlers;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
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
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.jspecify.annotations.Nullable;

@Decorator
@Priority(50)
public class AuthorizationRequestHandlerDecorator implements RequestHandler {

    @Inject
    @Delegate
    @Any
    private RequestHandler delegate;

    @Inject
    @Any
    Instance<TaskAuthorizationProvider> authorizationProviderInstance;

    private @Nullable TaskAuthorizationProvider authorizationProvider;

    public AuthorizationRequestHandlerDecorator() {
    }

    AuthorizationRequestHandlerDecorator(RequestHandler delegate,
            @Nullable TaskAuthorizationProvider authorizationProvider) {
        this.delegate = delegate;
        this.authorizationProvider = authorizationProvider;
    }

    @PostConstruct
    void init() {
        if (authorizationProviderInstance != null) {
            authorizationProvider = authorizationProviderInstance.isResolvable()
                    ? authorizationProviderInstance.get()
                    : null;
        }
    }

    private Flow.Publisher<StreamingEventKind> wrapPublisherForOwnership(
            Flow.Publisher<StreamingEventKind> publisher,
            ServerCallContext context,
            TaskOperation operation,
            TaskAuthorizationProvider provider) {
        return subscriber -> publisher.subscribe(new Flow.Subscriber<>() {
            private final AtomicBoolean ownershipChecked = new AtomicBoolean(false);
            private final AtomicBoolean done = new AtomicBoolean(false);
            @SuppressWarnings("NullAway")
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription s) {
                this.subscription = s;
                subscriber.onSubscribe(s);
            }

            @Override
            public void onNext(StreamingEventKind event) {
                if (done.get()) {
                    return;
                }
                if (!ownershipChecked.get()) {
                    String taskId = extractTaskId(event);
                    if (taskId != null) {
                        ownershipChecked.set(true);
                        try {
                            if (!provider.isTaskRecorded(taskId)) {
                                provider.recordOwnership(context, taskId, operation);
                            }
                        } catch (A2AError e) {
                            done.set(true);
                            subscription.cancel();
                            subscriber.onError(e);
                            return;
                        }
                    }
                }
                subscriber.onNext(event);
            }

            @Override
            public void onError(Throwable t) {
                if (done.compareAndSet(false, true)) {
                    subscriber.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (done.compareAndSet(false, true)) {
                    subscriber.onComplete();
                }
            }
        });
    }

    private void enforceCreate(ServerCallContext context, TaskOperation operation) throws A2AError {
        if (authorizationProvider != null && !authorizationProvider.checkCreate(context, operation)) {
            throw new TaskNotFoundError();
        }
    }

    private @Nullable String extractTaskId(Object event) {
        if (event instanceof Task task) {
            return task.id();
        } else if (event instanceof TaskStatusUpdateEvent e) {
            return e.taskId();
        } else if (event instanceof TaskArtifactUpdateEvent e) {
            return e.taskId();
        }
        return null;
    }

    private void recordOwnershipIfNeeded(ServerCallContext context, @Nullable String taskId,
            TaskOperation operation) throws A2AError {
        if (authorizationProvider != null && taskId != null && !authorizationProvider.isTaskRecorded(taskId)) {
            authorizationProvider.recordOwnership(context, taskId, operation);
        }
    }

    private void enforceWrite(ServerCallContext context, String taskId, TaskOperation operation) throws A2AError {
        if (authorizationProvider != null && !authorizationProvider.checkWrite(context, taskId, operation)) {
            throw new TaskNotFoundError();
        }
    }

    private void enforceRead(ServerCallContext context, String taskId, TaskOperation operation) throws A2AError {
        if (authorizationProvider != null && !authorizationProvider.checkRead(context, taskId, operation)) {
            throw new TaskNotFoundError();
        }
    }

    @Override
    public Task onGetTask(TaskQueryParams params, ServerCallContext context) throws A2AError {
        enforceRead(context, params.id(), TaskOperation.GET_TASK);
        return delegate.onGetTask(params, context);
    }

    @Override
    public ListTasksResult onListTasks(ListTasksParams params, ServerCallContext context) throws A2AError {
        return delegate.onListTasks(params, context);
    }

    @Override
    public Task onCancelTask(CancelTaskParams params, ServerCallContext context) throws A2AError {
        enforceWrite(context, params.id(), TaskOperation.CANCEL_TASK);
        return delegate.onCancelTask(params, context);
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws A2AError {
        String taskId = params.message().taskId();
        if (taskId != null) {
            enforceWrite(context, taskId, TaskOperation.MESSAGE_SEND);
        } else {
            enforceCreate(context, TaskOperation.MESSAGE_SEND);
        }
        EventKind result = delegate.onMessageSend(params, context);
        String resultTaskId = extractTaskId(result);
        recordOwnershipIfNeeded(context, resultTaskId, TaskOperation.MESSAGE_SEND);
        return result;
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(MessageSendParams params,
            ServerCallContext context) throws A2AError {
        String taskId = params.message().taskId();
        if (taskId != null) {
            enforceWrite(context, taskId, TaskOperation.MESSAGE_SEND_STREAM);
        } else {
            enforceCreate(context, TaskOperation.MESSAGE_SEND_STREAM);
        }
        Flow.Publisher<StreamingEventKind> publisher = delegate.onMessageSendStream(params, context);
        if (authorizationProvider != null) {
            publisher = wrapPublisherForOwnership(publisher, context, TaskOperation.MESSAGE_SEND_STREAM,
                    authorizationProvider);
        }
        return publisher;
    }

    @Override
    public TaskPushNotificationConfig onCreateTaskPushNotificationConfig(TaskPushNotificationConfig params,
            ServerCallContext context) throws A2AError {
        String taskId = params.taskId();
        if (taskId != null) {
            enforceWrite(context, taskId, TaskOperation.CREATE_TASK_PUSH_NOTIFICATION_CONFIG);
        }
        // taskId is required by the spec; if null, the delegate will reject with InvalidParamsError
        return delegate.onCreateTaskPushNotificationConfig(params, context);
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(GetTaskPushNotificationConfigParams params,
            ServerCallContext context) throws A2AError {
        enforceRead(context, params.taskId(), TaskOperation.GET_TASK_PUSH_NOTIFICATION_CONFIG);
        return delegate.onGetTaskPushNotificationConfig(params, context);
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onSubscribeToTask(TaskIdParams params,
            ServerCallContext context) throws A2AError {
        enforceRead(context, params.id(), TaskOperation.SUBSCRIBE_TO_TASK);
        return delegate.onSubscribeToTask(params, context);
    }

    @Override
    public ListTaskPushNotificationConfigsResult onListTaskPushNotificationConfigs(
            ListTaskPushNotificationConfigsParams params, ServerCallContext context) throws A2AError {
        enforceRead(context, params.id(), TaskOperation.LIST_TASK_PUSH_NOTIFICATION_CONFIGS);
        return delegate.onListTaskPushNotificationConfigs(params, context);
    }

    @Override
    public void onDeleteTaskPushNotificationConfig(DeleteTaskPushNotificationConfigParams params,
            ServerCallContext context) throws A2AError {
        enforceWrite(context, params.taskId(), TaskOperation.DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
        delegate.onDeleteTaskPushNotificationConfig(params, context);
    }

    @Override
    public void validateRequestedTask(@Nullable String requestedTaskId) throws A2AError {
        delegate.validateRequestedTask(requestedTaskId);
    }
}
