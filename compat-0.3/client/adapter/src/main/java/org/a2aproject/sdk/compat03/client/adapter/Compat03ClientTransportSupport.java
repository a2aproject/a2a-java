package org.a2aproject.sdk.compat03.client.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.EventKindMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.StreamingEventKindMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskPushNotificationConfigMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.CancelTaskParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.MessageSendParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.TaskIdParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.params.TaskQueryParamsMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.result.ListTaskPushNotificationConfigsResultMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.jspecify.annotations.Nullable;

/** Shared, binding-independent behavior for adapters from the 1.0 client to 0.3 transports. */
public final class Compat03ClientTransportSupport {

    private Compat03ClientTransportSupport() {
    }

    public static void validateParameters(Map<String, ?> parameters) {
        if (!parameters.isEmpty()) {
            throw new A2AClientException("0.3 client adapters do not support generic transport parameters");
        }
    }

    public static void validateListTasks(@Nullable ListTasksParams request) {
        throw unsupported("listTasks");
    }

    public static void validateExtendedAgentCard(@Nullable Object request) {
        throw unsupported("getExtendedAgentCard");
    }

    public static void validateTenant(String operation, @Nullable String tenant) {
        if (tenant != null && !tenant.isEmpty()) {
            throw unsupported(operation + " with a tenant");
        }
    }

    public static void validateMessageSend(MessageSendParams request) {
        validateTenant("sendMessage", request.tenant());
    }

    public static void validateTaskQuery(TaskQueryParams request) {
        validateTenant("getTask", request.tenant());
    }

    public static void validateCancel(CancelTaskParams request) {
        validateTenant("cancelTask", request.tenant());
    }

    public static void validatePushConfig(TaskPushNotificationConfig request) {
        validateTenant("createTaskPushNotificationConfiguration", request.tenant());
    }

    public static void validateGetPush(org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams request) {
        validateTenant("getTaskPushNotificationConfiguration", request.tenant());
    }

    public static void validateDeletePush(org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams request) {
        validateTenant("deleteTaskPushNotificationConfigurations", request.tenant());
    }

    public static void validatePushList(ListTaskPushNotificationConfigsParams request) {
        validateTenant("listTaskPushNotificationConfigurations", request.tenant());
        if (request.pageSize() != 0 || (request.pageToken() != null && !request.pageToken().isEmpty())) {
            throw unsupported("listTaskPushNotificationConfigurations pagination");
        }
    }

    public static void validateConfig(ClientTransportConfig<?> config) {
        validateParameters(config.getParameters());
    }

    public static ClientCallContext_v0_3 toV03Context(@Nullable ClientCallContext context) {
        if (context == null) {
            return null;
        }
        return Compat03ClientCallContextMapper.toV03(context);
    }

    public static MessageSendParams_v0_3 toV03(MessageSendParams request) {
        return MessageSendParamsMapper_v0_3.INSTANCE.fromV10(request);
    }

    public static TaskQueryParams_v0_3 toV03(TaskQueryParams request) {
        return TaskQueryParamsMapper_v0_3.INSTANCE.fromV10(request);
    }

    public static TaskIdParams_v0_3 toV03(TaskIdParams request) {
        return TaskIdParamsMapper_v0_3.INSTANCE.fromV10(request);
    }

    public static TaskIdParams_v0_3 toV03(org.a2aproject.sdk.spec.CancelTaskParams request) {
        return CancelTaskParamsMapper_v0_3.INSTANCE.fromV10(request);
    }

    public static TaskPushNotificationConfig_v0_3 toV03(TaskPushNotificationConfig request) {
        return TaskPushNotificationConfigMapper_v0_3.INSTANCE.fromV10(request);
    }

    public static GetTaskPushNotificationConfigParams_v0_3 toV03(
            org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams request) {
        return new GetTaskPushNotificationConfigParams_v0_3(request.taskId(), request.id());
    }

    public static ListTaskPushNotificationConfigParams_v0_3 toV03(
            ListTaskPushNotificationConfigsParams request) {
        return new ListTaskPushNotificationConfigParams_v0_3(request.id());
    }

    public static DeleteTaskPushNotificationConfigParams_v0_3 toV03(
            org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams request) {
        return new DeleteTaskPushNotificationConfigParams_v0_3(request.taskId(), request.id());
    }

    public static org.a2aproject.sdk.spec.EventKind toV10(
            org.a2aproject.sdk.compat03.spec.EventKind_v0_3 event) {
        return EventKindMapper_v0_3.INSTANCE.toV10(event);
    }

    public static StreamingEventKind toV10(StreamingEventKind_v0_3 event) {
        return StreamingEventKindMapper_v0_3.INSTANCE.toV10(event);
    }

    public static TaskPushNotificationConfig toV10(TaskPushNotificationConfig_v0_3 config) {
        return TaskPushNotificationConfigMapper_v0_3.INSTANCE.toV10(config);
    }

    public static ListTaskPushNotificationConfigsResult toV10PushList(
            List<TaskPushNotificationConfig_v0_3> configs) {
        ListTaskPushNotificationConfigsResult result =
                ListTaskPushNotificationConfigsResultMapper_v0_3.INSTANCE.toV10(configs);
        return new ListTaskPushNotificationConfigsResult(result.configs(), "");
    }

    public static A2AClientException mapLegacyException(A2AClientException_v0_3 exception) {
        return Compat03ClientErrorMapper.toV10(exception);
    }

    public static Consumer<Throwable> mapAsyncError(Consumer<Throwable> errors) {
        return error -> errors.accept(error instanceof A2AClientException_v0_3 legacy
                ? mapLegacyException(legacy) : error);
    }

    public static <T> T call(ThrowingSupplier<T> delegate) {
        try {
            return delegate.get();
        } catch (A2AClientException_v0_3 exception) {
            throw mapLegacyException(exception);
        }
    }

    public static void run(ThrowingRunnable delegate) {
        try {
            delegate.run();
        } catch (A2AClientException_v0_3 exception) {
            throw mapLegacyException(exception);
        }
    }

    private static A2AClientException unsupported(String operation) {
        return new A2AClientException(operation + " is not supported by A2A protocol 0.3",
                new org.a2aproject.sdk.spec.UnsupportedOperationError());
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws A2AClientException_v0_3;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws A2AClientException_v0_3;
    }
}
