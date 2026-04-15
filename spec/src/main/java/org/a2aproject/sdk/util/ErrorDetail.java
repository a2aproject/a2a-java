package org.a2aproject.sdk.util;

import java.util.Map;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single entry in the JSON-RPC {@code error.data} array, following
 * the Google {@code ErrorInfo} format ({@code type.googleapis.com/google.rpc.ErrorInfo}).
 */
public record ErrorDetail(
        @SerializedName("@type") String type,
        String reason,
        String domain,
        @Nullable Map<String, Object> metadata) {

    public static final String ERROR_INFO_TYPE = "type.googleapis.com/google.rpc.ErrorInfo";
    public static final String ERROR_DOMAIN = "a2a-protocol.org";

    public ErrorDetail {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("reason", reason);
        Assert.checkNotNullParam("domain", domain);
    }

    /** Convenience factory using the standard A2A ErrorInfo type and domain. */
    public static ErrorDetail of(String reason, @Nullable Map<String, Object> metadata) {
        return new ErrorDetail(ERROR_INFO_TYPE, reason, ERROR_DOMAIN, metadata);
    }
}
