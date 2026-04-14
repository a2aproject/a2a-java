package org.a2aproject.sdk.extras.opentelemetry;

import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.ERROR_TYPE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_REQUEST_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.EXTRACT_RESPONSE_SYS_PROPERTY;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONFIG_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_EXTENSIONS;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_MESSAGE_ID;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_PARTS_NUMBER;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_REQUEST;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_RESPONSE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_ROLE;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_TASK_ID;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AMethods;
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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.util.concurrent.Flow;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenTelemetry CDI Decorator for {@link RequestHandler}.
 * <p>
 * This decorator adds distributed tracing to A2A server request handlers.
 * It creates spans for each request handler method invocation, capturing:
 * <ul>
 *   <li>Request parameters as span attributes</li>
 *   <li>Response data as span attributes</li>
 *   <li>Errors and exceptions with proper status codes</li>
 *   <li>Timing information for performance monitoring</li>
 * </ul>
 * <p>
 * To enable this decorator, add it to your beans.xml:
 * <pre>{@code
 * <decorators>
 *     <class>org.a2aproject.sdk.extras.opentelemetry.OpenTelemetryRequestHandlerDecorator</class>
 * </decorators>
 * }</pre>
 */
@Decorator
@Priority(100)
public abstract class OpenTelemetryRequestHandlerDecorator implements RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryRequestHandlerDecorator.class);

    @Inject
    @Delegate
    @Any
    private RequestHandler delegate;

    @Inject
    private Tracer tracer;

    /**
     * Default constructor for CDI.
     */
    public OpenTelemetryRequestHandlerDecorator() {
    }

    /**
     * Constructor for testing.
     *
     * @param delegate the delegate request handler
     * @param tracer the tracer to use
     */
    public OpenTelemetryRequestHandlerDecorator(RequestHandler delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public Task onGetTask(TaskQueryParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_METHOD);

        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.onGetTask(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public ListTasksResult onListTasks(ListTasksParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_METHOD);

        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }
        if (params.contextId() != null) {
            spanBuilder.setAttribute(GENAI_CONTEXT_ID, params.contextId());
        }
        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            ListTasksResult result = delegate.onListTasks(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public Task onCancelTask(CancelTaskParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.CANCEL_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.CANCEL_TASK_METHOD);

        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            Task result = delegate.onCancelTask(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public EventKind onMessageSend(MessageSendParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SEND_MESSAGE_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SEND_MESSAGE_METHOD);

        if (params.message() != null) {
            if (params.message().taskId() != null) {
                spanBuilder.setAttribute(GENAI_TASK_ID, params.message().taskId());
            }
            if (params.message().contextId() != null) {
                spanBuilder.setAttribute(GENAI_CONTEXT_ID, params.message().contextId());
            }
            if (params.message().messageId() != null) {
                spanBuilder.setAttribute(GENAI_MESSAGE_ID, params.message().messageId());
            }
            if (params.message().role() != null) {
                spanBuilder.setAttribute(GENAI_ROLE, params.message().role().name());
            }
            if (params.message().extensions() != null && !params.message().extensions().isEmpty()) {
                spanBuilder.setAttribute(GENAI_EXTENSIONS, String.join(",", params.message().extensions()));
            }
            spanBuilder.setAttribute(GENAI_PARTS_NUMBER, params.message().parts().size());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            EventKind result = delegate.onMessageSend(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onMessageSendStream(MessageSendParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SEND_STREAMING_MESSAGE_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SEND_STREAMING_MESSAGE_METHOD);

        if (params.message() != null) {
            if (params.message().taskId() != null) {
                spanBuilder.setAttribute(GENAI_TASK_ID, params.message().taskId());
            }
            if (params.message().contextId() != null) {
                spanBuilder.setAttribute(GENAI_CONTEXT_ID, params.message().contextId());
            }
            if (params.message().messageId() != null) {
                spanBuilder.setAttribute(GENAI_MESSAGE_ID, params.message().messageId());
            }
            if (params.message().role() != null) {
                spanBuilder.setAttribute(GENAI_ROLE, params.message().role().name());
            }
            if (params.message().extensions() != null && !params.message().extensions().isEmpty()) {
                spanBuilder.setAttribute(GENAI_EXTENSIONS, String.join(",", params.message().extensions()));
            }
            spanBuilder.setAttribute(GENAI_PARTS_NUMBER, params.message().parts().size());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            Flow.Publisher<StreamingEventKind> result = delegate.onMessageSendStream(params, context);

            if (extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, "Stream publisher created");
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public TaskPushNotificationConfig onCreateTaskPushNotificationConfig(TaskPushNotificationConfig params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (params.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.taskId());
        }
        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.onCreateTaskPushNotificationConfig(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public TaskPushNotificationConfig onGetTaskPushNotificationConfig(GetTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (params.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.taskId());
        }
        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_CONFIG_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            TaskPushNotificationConfig result = delegate.onGetTaskPushNotificationConfig(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public Flow.Publisher<StreamingEventKind> onSubscribeToTask(TaskIdParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.SUBSCRIBE_TO_TASK_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.SUBSCRIBE_TO_TASK_METHOD);

        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }
        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            Flow.Publisher<StreamingEventKind> result = delegate.onSubscribeToTask(params, context);

            if (extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, "Stream publisher created");
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public ListTaskPushNotificationConfigsResult onListTaskPushNotificationConfigs(ListTaskPushNotificationConfigsParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }
        if (params.id() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.id());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            ListTaskPushNotificationConfigsResult result = delegate.onListTaskPushNotificationConfigs(params, context);

            if (result != null && extractResponse()) {
                span.setAttribute(GENAI_RESPONSE, result.toString());
            }

            span.setStatus(StatusCode.OK);
            return result;
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    @Override
    public void onDeleteTaskPushNotificationConfig(DeleteTaskPushNotificationConfigParams params, ServerCallContext context) throws A2AError {
        var spanBuilder = tracer.spanBuilder(A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(GENAI_OPERATION_NAME, A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        if (extractRequest()) {
            spanBuilder.setAttribute(GENAI_REQUEST, params.toString());
        }
        if (params.taskId() != null) {
            spanBuilder.setAttribute(GENAI_TASK_ID, params.taskId());
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            delegate.onDeleteTaskPushNotificationConfig(params, context);

            span.setStatus(StatusCode.OK);
        } catch (A2AError error) {
            span.setAttribute(ERROR_TYPE, error.getMessage());
            span.setStatus(StatusCode.ERROR, error.getMessage());
            throw error;
        } finally {
            span.end();
        }
    }

    public void validateRequestedTask(@Nullable String requestedTaskId) throws A2AError {
        delegate.validateRequestedTask(requestedTaskId);
    }

    private boolean extractRequest() {
        return Boolean.getBoolean(EXTRACT_REQUEST_SYS_PROPERTY);
    }

    private boolean extractResponse() {
        return Boolean.getBoolean(EXTRACT_RESPONSE_SYS_PROPERTY);
    }
}
