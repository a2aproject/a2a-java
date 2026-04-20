package org.a2aproject.sdk.compat03.conversion;

import java.util.List;
import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.a2aproject.sdk.compat03.conversion.mappers.domain.EventKindMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.StreamingEventKindMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskPushNotificationConfigMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.CancelTaskParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.MessageSendParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.TaskIdParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.TaskQueryParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.result.ListTaskPushNotificationConfigsResultMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;

/**
 * Request handler that converts v0.3 protocol requests to v1.0 and delegates to the v1.0 {@link RequestHandler}.
 * <p>
 * This class acts as an adapter layer between the v0.3 transport handlers and the v1.0 core request handler.
 * It accepts v0.3 spec types, converts them to v1.0 types, delegates to the v1.0 DefaultRequestHandler,
 * and converts the results back to v0.3 types.
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Convert v0.3 params to v1.0 params using mappers</li>
 *   <li>Delegate to v1.0 RequestHandler</li>
 *   <li>Convert v1.0 results back to v0.3 results</li>
 *   <li>Handle streaming publishers with element-by-element conversion</li>
 * </ul>
 * <p>
 * Method naming differences between v0.3 and v1.0:
 * <ul>
 *   <li>{@code onSetTaskPushNotificationConfig} (v0.3) → {@code onCreateTaskPushNotificationConfig} (v1.0)</li>
 *   <li>{@code onResubscribeToTask} (v0.3) → {@code onSubscribeToTask} (v1.0)</li>
 *   <li>{@code onListTaskPushNotificationConfig} (v0.3) → {@code onListTaskPushNotificationConfigs} (v1.0)</li>
 * </ul>
 */
@ApplicationScoped
public class Convert_v0_3_To10RequestHandler {

    /**
     * The v1.0 {@link RequestHandler} to which all converted requests are delegated.
     */
    @Inject
    public RequestHandler v10Handler;

    /**
     * Gets a task by ID.
     * <p>
     * v0.3 → v1.0: Converts TaskQueryParams and Task
     *
     * @param v03Params the v0.3 task query params
     * @param context the server call context
     * @return the v0.3 task
     * @throws A2AError if an error occurs
     */
    public Task_v0_3 onGetTask(
            TaskQueryParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params
        TaskQueryParams v10Params = TaskQueryParamsMapper_v0_3.INSTANCE.toV10(v03Params);

        // Call v1.0 handler
        Task v10Result = v10Handler.onGetTask(v10Params, context);

        // Convert v1.0 result → v0.3 result
        return TaskMapper_v0_3.INSTANCE.fromV10(v10Result);
    }

    /**
     * Cancels a task.
     * <p>
     * v0.3 → v1.0: Converts TaskIdParams to CancelTaskParams and Task
     *
     * @param v03Params the v0.3 task ID params
     * @param context the server call context
     * @return the v0.3 task
     * @throws A2AError if an error occurs
     */
    public Task_v0_3 onCancelTask(
            TaskIdParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 TaskIdParams → v1.0 CancelTaskParams
        CancelTaskParams v10Params = CancelTaskParamsMapper_v0_3.INSTANCE.toV10(v03Params);

        // Call v1.0 handler
        Task v10Result = v10Handler.onCancelTask(v10Params, context);

        // Convert v1.0 result → v0.3 result
        return TaskMapper_v0_3.INSTANCE.fromV10(v10Result);
    }

    /**
     * Sends a message (blocking).
     * <p>
     * v0.3 → v1.0: Converts MessageSendParams and EventKind
     *
     * @param v03Params the v0.3 message send params
     * @param context the server call context
     * @return the v0.3 event kind (Task or Message)
     * @throws A2AError if an error occurs
     */
    public EventKind_v0_3 onMessageSend(
            MessageSendParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params
        MessageSendParams v10Params = MessageSendParamsMapper_v0_3.INSTANCE.toV10(v03Params);

        // Enable v0.3 compatibility mode: disable strict context ID validation
        // v0.3 allowed messages with both taskId and contextId even if they didn't match
        context.getState().put(ServerCallContext.STRICT_CONTEXT_VALIDATION_KEY, false);

        // Call v1.0 handler
        EventKind v10Result = v10Handler.onMessageSend(v10Params, context);

        // Convert v1.0 result → v0.3 result
        return EventKindMapper_v0_3.INSTANCE.fromV10(v10Result);
    }

    /**
     * Sends a message (streaming).
     * <p>
     * v0.3 → v1.0: Converts MessageSendParams and streams StreamingEventKind
     *
     * @param v03Params the v0.3 message send params
     * @param context the server call context
     * @return publisher of v0.3 streaming event kinds
     * @throws A2AError if an error occurs
     */
    public Flow.Publisher<StreamingEventKind_v0_3> onMessageSendStream(
            MessageSendParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params
        MessageSendParams v10Params = MessageSendParamsMapper_v0_3.INSTANCE.toV10(v03Params);

        // Enable v0.3 compatibility mode: disable strict context ID validation
        // v0.3 allowed messages with both taskId and contextId even if they didn't match
        context.getState().put(ServerCallContext.STRICT_CONTEXT_VALIDATION_KEY, false);

        // Get v1.0 publisher
        Flow.Publisher<StreamingEventKind> v10Publisher =
            v10Handler.onMessageSendStream(v10Params, context);

        // Convert each event using a mapping processor
        return convertPublisher(v10Publisher, StreamingEventKindMapper_v0_3.INSTANCE::fromV10);
    }

    /**
     * Sets (creates) a task push notification configuration.
     * <p>
     * v0.3 method name: {@code onSetTaskPushNotificationConfig}
     * v1.0 method name: {@code onCreateTaskPushNotificationConfig}
     *
     * @param v03Config the v0.3 task push notification config
     * @param context the server call context
     * @return the v0.3 task push notification config
     * @throws A2AError if an error occurs
     */
    public TaskPushNotificationConfig_v0_3 onSetTaskPushNotificationConfig(
            TaskPushNotificationConfig_v0_3 v03Config,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 config → v1.0 config
        TaskPushNotificationConfig v10Config =
            TaskPushNotificationConfigMapper_v0_3.INSTANCE.toV10(v03Config);

        // Call v1.0 handler
        TaskPushNotificationConfig v10Result =
            v10Handler.onCreateTaskPushNotificationConfig(v10Config, context);

        // Convert v1.0 result → v0.3 result
        return TaskPushNotificationConfigMapper_v0_3.INSTANCE.fromV10(v10Result);
    }

    /**
     * Gets a task push notification configuration.
     * <p>
     * v0.3 → v1.0: Converts GetTaskPushNotificationConfigParams and TaskPushNotificationConfig
     * <p>
     * v0.3 semantics: If a specific pushNotificationConfigId is provided but not found,
     * fall back to returning the first config (lenient behavior).
     *
     * @param v03Params the v0.3 get params
     * @param context the server call context
     * @return the v0.3 task push notification config
     * @throws A2AError if an error occurs
     */
    public TaskPushNotificationConfig_v0_3 onGetTaskPushNotificationConfig(
            GetTaskPushNotificationConfigParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params
        // v0.3: id = taskId, pushNotificationConfigId = optional config id
        // v1.0: taskId = taskId, id = config id (defaults to taskId if not specified)
        String configId = v03Params.pushNotificationConfigId() != null
            ? v03Params.pushNotificationConfigId()
            : v03Params.id(); // Default to taskId when config id not specified

        GetTaskPushNotificationConfigParams v10Params =
            new GetTaskPushNotificationConfigParams(v03Params.id(), configId);

        TaskPushNotificationConfig v10Result;
        try {
            // Call v1.0 handler
            v10Result = v10Handler.onGetTaskPushNotificationConfig(v10Params, context);
        } catch (TaskNotFoundError e) {
            // v0.3 fallback behavior: if specific config ID not found, return first config
            // This matches v0.3 DefaultRequestHandler.getPushNotificationConfig() behavior
            if (v03Params.pushNotificationConfigId() != null) {
                ListTaskPushNotificationConfigsParams listParams =
                    new ListTaskPushNotificationConfigsParams(v03Params.id());
                ListTaskPushNotificationConfigsResult listResult =
                    v10Handler.onListTaskPushNotificationConfigs(listParams, context);

                if (listResult.configs().isEmpty()) {
                    throw e; // Re-throw if no configs exist at all
                }
                // Return first config as fallback
                v10Result = listResult.configs().get(0);
            } else {
                throw e; // Re-throw if no fallback is applicable
            }
        }

        // Convert v1.0 result → v0.3 result
        return TaskPushNotificationConfigMapper_v0_3.INSTANCE.fromV10(v10Result);
    }

    /**
     * Resubscribes to task updates (streaming).
     * <p>
     * v0.3 method name: {@code onResubscribeToTask}
     * v1.0 method name: {@code onSubscribeToTask}
     *
     * @param v03Params the v0.3 task ID params
     * @param context the server call context
     * @return publisher of v0.3 streaming event kinds
     * @throws A2AError if an error occurs
     */
    public Flow.Publisher<StreamingEventKind_v0_3> onResubscribeToTask(
            TaskIdParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params
        TaskIdParams v10Params = TaskIdParamsMapper_v0_3.INSTANCE.toV10(v03Params);

        // Get v1.0 publisher
        Flow.Publisher<StreamingEventKind> v10Publisher =
            v10Handler.onSubscribeToTask(v10Params, context);

        // Convert each event using a mapping processor
        return convertPublisher(v10Publisher, StreamingEventKindMapper_v0_3.INSTANCE::fromV10);
    }

    /**
     * Lists task push notification configurations.
     * <p>
     * v0.3 → v1.0: Converts params and result (List → ListTaskPushNotificationConfigsResult)
     *
     * @param v03Params the v0.3 list params
     * @param context the server call context
     * @return list of v0.3 task push notification configs
     * @throws A2AError if an error occurs
     */
    public List<TaskPushNotificationConfig_v0_3> onListTaskPushNotificationConfig(
            ListTaskPushNotificationConfigParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params
        // ListTaskPushNotificationConfigParams has different structure - v0.3 has id, v1.0 has more fields
        ListTaskPushNotificationConfigsParams v10Params =
            new ListTaskPushNotificationConfigsParams(
                v03Params.id(),
                0,      // No pageSize in v0.3 - use 0 (will use default)
                "",     // No pageToken in v0.3 - use empty string
                ""      // Default tenant
            );

        // Call v1.0 handler
        ListTaskPushNotificationConfigsResult v10Result =
            v10Handler.onListTaskPushNotificationConfigs(v10Params, context);

        // Convert v1.0 result → v0.3 result (extract list from result wrapper)
        return ListTaskPushNotificationConfigsResultMapper_v0_3.INSTANCE.fromV10(v10Result);
    }

    /**
     * Deletes a task push notification configuration.
     * <p>
     * v0.3 → v1.0: Converts DeleteTaskPushNotificationConfigParams (adds tenant field)
     *
     * @param v03Params the v0.3 delete params
     * @param context the server call context
     * @throws A2AError if an error occurs
     */
    public void onDeleteTaskPushNotificationConfig(
            DeleteTaskPushNotificationConfigParams_v0_3 v03Params,
            ServerCallContext context) throws A2AError {

        // Convert v0.3 params → v1.0 params (add tenant field)
        DeleteTaskPushNotificationConfigParams v10Params =
            new DeleteTaskPushNotificationConfigParams(
                v03Params.id(),
                v03Params.pushNotificationConfigId(),
                ""  // Default tenant
            );

        // Call v1.0 handler
        v10Handler.onDeleteTaskPushNotificationConfig(v10Params, context);
    }

    /**
     * Converts a v1.0 publisher to a v0.3 publisher by applying a mapper to each element.
     *
     * @param v10Publisher the v1.0 publisher
     * @param mapper function to convert each v1.0 element to v0.3
     * @param <V10> the v1.0 element type
     * @param <V03> the v0.3 element type
     * @return publisher of v0.3 elements
     */
    private <V10, V03> Flow.Publisher<V03> convertPublisher(
            Flow.Publisher<V10> v10Publisher,
            java.util.function.Function<V10, V03> mapper) {

        return subscriber -> v10Publisher.subscribe(new Flow.Subscriber<V10>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(V10 v10Item) {
                try {
                    V03 v03Item = mapper.apply(v10Item);
                    subscriber.onNext(v03Item);
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
    }
}
