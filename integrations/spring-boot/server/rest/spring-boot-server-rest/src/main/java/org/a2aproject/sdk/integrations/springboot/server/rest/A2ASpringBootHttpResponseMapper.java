package org.a2aproject.sdk.integrations.springboot.server.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.grpc.utils.ProtoJsonUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AErrorCodes;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.util.ErrorDetail;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.jspecify.annotations.Nullable;

/**
 * Serializes A2A runtime results into HTTP response bodies for Spring MVC.
 *
 * <p>The mapper keeps protocol serialization in one place so the controller can stay thin and
 * focused on request extraction and delegation. It also owns the SSE conversion for streaming
 * task responses.
 */
public final class A2ASpringBootHttpResponseMapper {

    private static final JsonFormat.Printer DEFAULT_PRINTER = JsonFormat.printer().alwaysPrintFieldsWithNoPresence();
    private static final JsonFormat.Printer STREAM_PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();

    ResponseEntity<String> ok(Object body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(body));
    }

    ResponseEntity<String> ok(Object body, Map<String, String> headers) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON);
        headers.forEach(builder::header);
        return builder.body(toJson(body));
    }

    ResponseEntity<String> created(Object body) {
        return ResponseEntity.status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(body));
    }

    ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    ResponseEntity<String> error(A2AError error) {
        int statusCode = mapErrorToHttpStatus(error);
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(new ErrorResponse(error)));
    }

    ResponseEntity<String> error(Throwable throwable) {
        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName();
        return error(new InternalError(message));
    }

    ResponseEntity<String> okTask(Task task) {
        return okProto(ProtoUtils.ToProto.task(task));
    }

    ResponseEntity<String> okSendMessage(EventKind event) {
        return okProto(ProtoUtils.ToProto.taskOrMessage(event));
    }

    ResponseEntity<String> okListTasks(ListTasksResult result) {
        return okProto(ProtoUtils.ToProto.listTasksResult(result));
    }

    ResponseEntity<String> okTaskPushNotificationConfig(TaskPushNotificationConfig config) {
        return okProto(ProtoUtils.ToProto.taskPushNotificationConfig(config));
    }

    ResponseEntity<String> createdTaskPushNotificationConfig(TaskPushNotificationConfig config) {
        return createdProto(ProtoUtils.ToProto.taskPushNotificationConfig(config));
    }

    ResponseEntity<String> okListTaskPushNotificationConfigs(ListTaskPushNotificationConfigsResult result) {
        return okProto(ProtoUtils.ToProto.listTaskPushNotificationConfigsResponse(result));
    }

    SseEmitter toSseEmitter(Flow.Publisher<StreamingEventKind> publisher, ServerCallContext context) {
        SseEmitter emitter = new SseEmitter(0L);
        java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Flow.Subscription> subscriptionRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        context.setEventConsumerCancelCallback(() -> {
            java.util.concurrent.Flow.Subscription subscription = subscriptionRef.get();
            if (subscription != null) {
                subscription.cancel();
            }
        });
        emitter.onCompletion(context::invokeEventConsumerCancelCallback);
        emitter.onTimeout(context::invokeEventConsumerCancelCallback);
        emitter.onError(throwable -> context.invokeEventConsumerCancelCallback());

        publisher.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscriptionRef.set(subscription);
                subscription.request(1);
            }

            @Override
            public void onNext(StreamingEventKind item) {
                try {
                    emitter.send(SseEmitter.event().data(toProtoJson(ProtoUtils.ToProto.taskOrMessageStream(item))));
                } catch (IOException e) {
                    java.util.concurrent.Flow.Subscription subscription = subscriptionRef.get();
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    emitter.completeWithError(e);
                    return;
                }
                java.util.concurrent.Flow.Subscription subscription = subscriptionRef.get();
                if (subscription != null) {
                    subscription.request(1);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.completeWithError(throwable);
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }
        });
        return emitter;
    }

    private String toJson(Object value) {
        try {
            return JsonUtil.toJson(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize A2A response", e);
        }
    }

    private static int mapErrorToHttpStatus(A2AError error) {
        A2AErrorCodes errorCode = A2AErrorCodes.fromCode(error.getCode());
        if (errorCode != null) {
            return errorCode.httpCode();
        }
        return A2AErrorCodes.INTERNAL.httpCode();
    }

    private record ErrorResponse(ErrorBody error) {
        private ErrorResponse(A2AError error) {
            this(new ErrorBody(error));
        }
    }

    private ResponseEntity<String> okProto(com.google.protobuf.MessageOrBuilder proto) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toProtoJson(DEFAULT_PRINTER, proto));
    }

    private ResponseEntity<String> createdProto(com.google.protobuf.MessageOrBuilder proto) {
        return ResponseEntity.status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toProtoJson(DEFAULT_PRINTER, proto));
    }

    private String toProtoJson(com.google.protobuf.MessageOrBuilder proto) {
        return toProtoJson(STREAM_PRINTER, proto);
    }

    private String toProtoJson(JsonFormat.Printer printer, com.google.protobuf.MessageOrBuilder proto) {
        try {
            return ProtoJsonUtils.toJson(printer, proto);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Failed to serialize A2A protobuf response", e);
        }
    }

    private record ErrorBody(int code, String status, String message, List<ErrorDetail> details) {
        private ErrorBody(A2AError error) {
            this(
                    mapErrorToHttpStatus(error),
                    A2AErrorCodes.fromCode(error.getCode()) != null
                            ? A2AErrorCodes.fromCode(error.getCode()).grpcStatus()
                            : A2AErrorCodes.INTERNAL.grpcStatus(),
                    error.getMessage() == null ? error.getClass().getName() : error.getMessage(),
                    List.of(ErrorDetail.of(
                            A2AErrorCodes.fromCode(error.getCode()) != null
                                    ? A2AErrorCodes.fromCode(error.getCode()).name()
                                    : A2AErrorCodes.INTERNAL.name(),
                            error.getDetails()))
            );
        }
    }
}
