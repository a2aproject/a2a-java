package org.a2aproject.sdk.compat03.spec;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents a JWS signature of an AgentCard.
 * This follows the JSON format of an RFC 7515 JSON Web Signature (JWS).
 */
public record AgentCardSignature_v0_3(Map<String, Object> header, @SerializedName("protected")String protectedHeader,
                                      String signature) {

    public AgentCardSignature_v0_3 {
        Assert.checkNotNullParam("protectedHeader", protectedHeader);
        Assert.checkNotNullParam("signature", signature);
    }

    public static class Builder {
        private Map<String, Object> header;
        String protectedHeader;
        String signature;

        public Builder header(Map<String, Object> header) {
            this.header = header;
            return this;
        }

        public Builder protectedHeader(String protectedHeader) {
            this.protectedHeader = protectedHeader;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public AgentCardSignature_v0_3 build() {
            return new AgentCardSignature_v0_3(header, protectedHeader, signature);
        }
    }
}
