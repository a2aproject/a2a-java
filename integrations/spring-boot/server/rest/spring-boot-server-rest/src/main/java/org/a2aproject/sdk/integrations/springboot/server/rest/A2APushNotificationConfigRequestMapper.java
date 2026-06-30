package org.a2aproject.sdk.integrations.springboot.server.rest;

import com.google.gson.JsonObject;

import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.jspecify.annotations.Nullable;

/**
 * Parses push-notification create requests into the A2A domain DTOs.
 *
 * <p>The mapper handles the small amount of request-body validation that is awkward to express
 * directly in controller signatures: path/body ID consistency, tenant consistency, and the
 * optional nested authentication payload.
 */
final class A2APushNotificationConfigRequestMapper {

    TaskPushNotificationConfig parseCreateRequest(String body, String taskId, @Nullable String tenant) {
        if (body == null || body.isBlank()) {
            throw new InvalidRequestError("Request body is required");
        }
        JsonObject jsonObject = JsonUtil.OBJECT_MAPPER.fromJson(body, JsonObject.class);
        if (jsonObject == null) {
            throw new InvalidRequestError("Request body is required");
        }

        String bodyTaskId = readOptionalString(jsonObject, "taskId");
        if (!bodyTaskId.isBlank() && !bodyTaskId.equals(taskId)) {
            throw new InvalidParamsError("Task ID in request body (" + bodyTaskId
                    + ") does not match task ID in URL path (" + taskId + ").");
        }

        String bodyTenant = readOptionalString(jsonObject, "tenant");
        if (!bodyTenant.isBlank() && tenant != null && !tenant.isBlank() && !bodyTenant.equals(tenant)) {
            throw new InvalidParamsError("Tenant in request body (" + bodyTenant
                    + ") does not match tenant in URL path (" + tenant + ").");
        }

        AuthenticationInfo authentication = null;
        if (jsonObject.has("authentication") && jsonObject.get("authentication").isJsonObject()) {
            authentication = deserialize(jsonObject.get("authentication").toString(), AuthenticationInfo.class);
        }

        String effectiveTenant = tenant;
        if (effectiveTenant == null || effectiveTenant.isBlank()) {
            effectiveTenant = bodyTenant.isBlank() ? null : bodyTenant;
        }

        return TaskPushNotificationConfig.builder()
                .id(readOptionalString(jsonObject, "id"))
                .taskId(taskId)
                .url(readRequiredString(jsonObject, "url"))
                .token(readNullableString(jsonObject, "token"))
                .authentication(authentication)
                .tenant(effectiveTenant)
                .build();
    }

    private String readRequiredString(JsonObject jsonObject, String fieldName) {
        String value = readOptionalString(jsonObject, fieldName);
        if (value.isBlank()) {
            throw new InvalidParamsError("Missing required field: " + fieldName);
        }
        return value;
    }

    private String readOptionalString(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName) || jsonObject.get(fieldName).isJsonNull()) {
            return "";
        }
        return jsonObject.get(fieldName).getAsString();
    }

    private @Nullable String readNullableString(JsonObject jsonObject, String fieldName) {
        String value = readOptionalString(jsonObject, fieldName);
        return value.isBlank() ? null : value;
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return JsonUtil.fromJson(json, type);
        } catch (org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException e) {
            throw new InvalidParamsError(e.getMessage());
        }
    }
}
