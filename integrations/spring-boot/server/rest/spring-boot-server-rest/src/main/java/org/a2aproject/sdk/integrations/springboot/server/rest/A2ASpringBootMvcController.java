package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.a2aproject.sdk.common.A2AHeaders.A2A_EXTENSIONS;
import static org.a2aproject.sdk.common.A2AHeaders.A2A_VERSION;
import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static org.a2aproject.sdk.server.ServerCallContext.EXECUTION_WRAPPER_KEY;
import static org.a2aproject.sdk.server.ServerCallContext.STRICT_CONTEXT_VALIDATION_KEY;
import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;
import static org.a2aproject.sdk.server.auth.UnauthenticatedUser.INSTANCE;
import static org.a2aproject.sdk.spec.TransportProtocol.HTTP_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import jakarta.servlet.http.HttpServletRequest;

import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.version.A2AVersionValidator;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Spring MVC transport adapter for A2A server runtime.
 *
 * <p>This controller keeps HTTP handling thin:
 * <ul>
 *   <li>extract request metadata into {@link ServerCallContext}</li>
 *   <li>delegate protocol work to {@link RequestHandler}</li>
 *   <li>serialize the A2A response/error contract back to JSON</li>
 * </ul>
 */
@RestController
public class A2ASpringBootMvcController {

    private final AgentCard agentCard;
    private final ObjectProvider<AgentCard> extendedAgentCard;
    private final RequestHandler requestHandler;
    private final A2ASpringBootHttpResponseMapper responseMapper;
    private final A2APushNotificationConfigRequestMapper pushNotificationConfigRequestMapper;
    private final org.springframework.beans.factory.ObjectProvider<StreamingSubscriptionObserver> streamingSubscriptionObserver;
    private final Instant startupTime = Instant.now();

    public A2ASpringBootMvcController(AgentCard agentCard,
            ObjectProvider<AgentCard> extendedAgentCard,
            RequestHandler requestHandler,
            A2ASpringBootHttpResponseMapper responseMapper,
            A2APushNotificationConfigRequestMapper pushNotificationConfigRequestMapper,
            org.springframework.beans.factory.ObjectProvider<StreamingSubscriptionObserver> streamingSubscriptionObserver) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.requestHandler = requestHandler;
        this.responseMapper = responseMapper;
        this.pushNotificationConfigRequestMapper = pushNotificationConfigRequestMapper;
        this.streamingSubscriptionObserver = streamingSubscriptionObserver;
    }

    @GetMapping(value = "/.well-known/agent-card.json", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAgentCard() {
        return responseMapper.ok(agentCard, createAgentCardHeaders());
    }

    @PostMapping(value = {"/message:send", "/{tenant}/message:send"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendMessage(@PathVariable(required = false) @Nullable String tenant,
            @RequestBody String body,
            HttpServletRequest request) {
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, SEND_MESSAGE_METHOD);
        A2AVersionValidator.validateProtocolVersion(agentCard, context);
        A2AExtensions.validateRequiredExtensions(agentCard, context);
        MessageSendParams params = deserialize(body, MessageSendParams.class);
        EventKind result = requestHandler.onMessageSend(withTenant(params, effectiveTenant), context);
        return responseMapper.okSendMessage(result);
    }

    @PostMapping(value = {"/message:stream", "/{tenant}/message:stream"}, produces = TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@PathVariable(required = false) @Nullable String tenant,
            @RequestBody String body,
            HttpServletRequest request) {
        requireStreamingSupported();
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, SEND_STREAMING_MESSAGE_METHOD);
        A2AVersionValidator.validateProtocolVersion(agentCard, context);
        A2AExtensions.validateRequiredExtensions(agentCard, context);
        MessageSendParams params = deserialize(body, MessageSendParams.class);
        MessageSendParams effectiveParams = withTenant(params, effectiveTenant);
        requestHandler.validateRequestedTask(effectiveParams.message().taskId());
        Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(effectiveParams, context);
        notifyStreamingSubscriptionStarted();
        return responseMapper.toSseEmitter(publisher, context);
    }

    @GetMapping(value = {"/tasks/{taskId}", "/{tenant}/tasks/{taskId}"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTask(@PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            @RequestParam(required = false) @Nullable Integer historyLength,
            HttpServletRequest request) {
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, GET_TASK_METHOD);
        Task result = requestHandler.onGetTask(new TaskQueryParams(taskId, historyLength, effectiveTenant), context);
        return responseMapper.okTask(result);
    }

    @GetMapping(value = {"/tasks", "/{tenant}/tasks"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listTasks(@PathVariable(required = false) @Nullable String tenant,
            @RequestParam(required = false) @Nullable String contextId,
            @RequestParam(required = false) @Nullable String status,
            @RequestParam(required = false) @Nullable Integer pageSize,
            @RequestParam(required = false) @Nullable String pageToken,
            @RequestParam(required = false) @Nullable Integer historyLength,
            @RequestParam(required = false) @Nullable String statusTimestampAfter,
            @RequestParam(required = false) @Nullable Boolean includeArtifacts,
            HttpServletRequest request) {
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, LIST_TASK_METHOD);
        ListTasksParams.Builder builder = ListTasksParams.builder()
                .contextId(contextId)
                .pageSize(pageSize)
                .pageToken(pageToken)
                .historyLength(historyLength)
                .includeArtifacts(includeArtifacts)
                .tenant(effectiveTenant);
        if (status != null && !status.isBlank()) {
            builder.status(TaskState.valueOf(status.toUpperCase()));
        }
        if (statusTimestampAfter != null && !statusTimestampAfter.isBlank()) {
            builder.statusTimestampAfter(Instant.parse(statusTimestampAfter));
        }
        return responseMapper.okListTasks(requestHandler.onListTasks(builder.build(), context));
    }

    @PostMapping(value = {"/tasks/{taskId}:cancel", "/{tenant}/tasks/{taskId}:cancel"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancelTask(@PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            @RequestBody(required = false) @Nullable String body,
            HttpServletRequest request) {
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, CANCEL_TASK_METHOD);
        Map<String, Object> metadata = body == null || body.isBlank() ? Map.of()
                : JsonUtil.readMetadata(JsonUtil.OBJECT_MAPPER.fromJson(body, com.google.gson.JsonObject.class));
        return responseMapper.okTask(requestHandler.onCancelTask(new CancelTaskParams(taskId, effectiveTenant, metadata), context));
    }

    @PostMapping(value = {"/tasks/{taskId}:subscribe", "/{tenant}/tasks/{taskId}:subscribe"}, produces = TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToTask(@PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            HttpServletRequest request) {
        requireStreamingSupported();
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, SUBSCRIBE_TO_TASK_METHOD);
        requestHandler.validateRequestedTask(taskId);
        Flow.Publisher<StreamingEventKind> publisher = requestHandler.onSubscribeToTask(
                new TaskIdParams(taskId, effectiveTenant), context);
        notifyStreamingSubscriptionStarted();
        return responseMapper.toSseEmitter(publisher, context);
    }

    @PostMapping(value = {"/tasks/{taskId}/pushNotificationConfigs", "/{tenant}/tasks/{taskId}/pushNotificationConfigs"},
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createTaskPushNotificationConfiguration(
            @PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            @RequestBody String body,
            HttpServletRequest request) {
        ensurePushNotificationsSupported();
        String effectiveTenant = normalizeTenant(tenant);
        TaskPushNotificationConfig params = pushNotificationConfigRequestMapper.parseCreateRequest(body, taskId, effectiveTenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        return responseMapper.createdTaskPushNotificationConfig(
                requestHandler.onCreateTaskPushNotificationConfig(params, context));
    }

    @GetMapping(value = {"/tasks/{taskId}/pushNotificationConfigs/{configId}",
            "/{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTaskPushNotificationConfiguration(
            @PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            @PathVariable String configId,
            HttpServletRequest request) {
        ensurePushNotificationsSupported();
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        return responseMapper.okTaskPushNotificationConfig(requestHandler.onGetTaskPushNotificationConfig(
                new GetTaskPushNotificationConfigParams(taskId, configId, effectiveTenant), context));
    }

    @GetMapping(value = {"/tasks/{taskId}/pushNotificationConfigs", "/{tenant}/tasks/{taskId}/pushNotificationConfigs"},
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listTaskPushNotificationConfigurations(
            @PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            @RequestParam(required = false) @Nullable Integer pageSize,
            @RequestParam(required = false) @Nullable String pageToken,
            HttpServletRequest request) {
        ensurePushNotificationsSupported();
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        return responseMapper.okListTaskPushNotificationConfigs(requestHandler.onListTaskPushNotificationConfigs(
                new ListTaskPushNotificationConfigsParams(taskId, pageSize == null ? 0 : pageSize,
                        pageToken == null ? "" : pageToken, effectiveTenant),
                context));
    }

    @DeleteMapping(value = {"/tasks/{taskId}/pushNotificationConfigs/{configId}",
            "/{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}"})
    public ResponseEntity<?> deleteTaskPushNotificationConfiguration(
            @PathVariable(required = false) @Nullable String tenant,
            @PathVariable String taskId,
            @PathVariable String configId,
            HttpServletRequest request) {
        ensurePushNotificationsSupported();
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        requestHandler.onDeleteTaskPushNotificationConfig(
                new DeleteTaskPushNotificationConfigParams(taskId, configId, effectiveTenant), context);
        return responseMapper.noContent();
    }

    @GetMapping(value = {"/extendedAgentCard", "/{tenant}/extendedAgentCard"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getExtendedAgentCard(@PathVariable(required = false) @Nullable String tenant,
            HttpServletRequest request) {
        String effectiveTenant = normalizeTenant(tenant);
        ServerCallContext context = createCallContext(request, effectiveTenant, GET_EXTENDED_AGENT_CARD_METHOD);
        if (agentCard.capabilities() == null || !agentCard.capabilities().extendedAgentCard()) {
            throw new UnsupportedOperationError();
        }
        AgentCard extendedCard = extendedAgentCard.getIfAvailable();
        if (extendedCard == null) {
            throw new ExtendedAgentCardNotConfiguredError(null, "Extended Card not configured", null);
        }
        return responseMapper.ok(extendedCard);
    }

    private MessageSendParams withTenant(MessageSendParams params, String tenant) {
        return MessageSendParams.builder()
                .message(params.message())
                .configuration(params.configuration())
                .metadata(params.metadata())
                .tenant(tenant)
                .build();
    }

    private ServerCallContext createCallContext(HttpServletRequest request, String tenant, String methodName) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Map<String, Object> state = new HashMap<>();
        state.put(TRANSPORT_KEY, HTTP_JSON);
        state.put(STRICT_CONTEXT_VALIDATION_KEY, Boolean.TRUE);
        state.put(EXECUTION_WRAPPER_KEY, createExecutionWrapper(requestAttributes));
        state.put("method", methodName);
        state.put("tenant", tenant);
        state.put("headers", readHeaders(request));

        List<String> requestedExtensions = readHeaderValues(request, A2A_EXTENSIONS);
        String requestedProtocolVersion = request.getHeader(A2A_VERSION);
        Principal principal = request.getUserPrincipal();
        var user = principal == null ? INSTANCE : new AuthenticatedUser(principal.getName());
        return new ServerCallContext(user, state, A2AExtensions.getRequestedExtensions(requestedExtensions),
                requestedProtocolVersion);
    }

    private String normalizeTenant(@Nullable String tenant) {
        return tenant == null ? "" : tenant;
    }

    private java.util.function.UnaryOperator<Runnable> createExecutionWrapper(@Nullable RequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return runnable -> runnable;
        }
        return runnable -> () -> {
            RequestAttributes previous = RequestContextHolder.getRequestAttributes();
            RequestContextHolder.setRequestAttributes(requestAttributes, false);
            try {
                runnable.run();
            } finally {
                if (previous == null) {
                    RequestContextHolder.resetRequestAttributes();
                } else {
                    RequestContextHolder.setRequestAttributes(previous, false);
                }
            }
        };
    }

    private Map<String, String> readHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private List<String> readHeaderValues(HttpServletRequest request, String headerName) {
        List<String> values = new ArrayList<>();
        Enumeration<String> headers = request.getHeaders(headerName);
        while (headers != null && headers.hasMoreElements()) {
            values.add(headers.nextElement());
        }
        return values;
    }

    private void ensurePushNotificationsSupported() throws PushNotificationNotSupportedError {
        if (agentCard.capabilities() == null || !agentCard.capabilities().pushNotifications()) {
            throw new PushNotificationNotSupportedError();
        }
    }

    private void requireStreamingSupported() {
        if (agentCard.capabilities() == null || !agentCard.capabilities().streaming()) {
            throw new InvalidRequestError("Streaming is not supported by the agent");
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new InvalidRequestError("Request body is required");
        }
        try {
            T result = JsonUtil.fromJson(json, type);
            if (result == null) {
                throw new InvalidRequestError("Request body is required");
            }
            return result;
        } catch (org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException e) {
            throw new InvalidParamsError(e.getMessage());
        }
    }

    private Map<String, String> createAgentCardHeaders() {
        return Map.of(
                "Cache-Control", "public, max-age=300",
                "ETag", "\"" + Integer.toHexString(agentCard.hashCode()) + "\"",
                "Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(startupTime)
        );
    }

    private void notifyStreamingSubscriptionStarted() {
        streamingSubscriptionObserver.ifAvailable(StreamingSubscriptionObserver::onStreamingSubscription);
    }
}
