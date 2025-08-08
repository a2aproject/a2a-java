package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Represents a JWS signature of an AgentCard.
 * This follows the JSON format of an RFC 7515 JSON Web Signature (JWS).
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCardSignature(Map<String, Object> header, @JsonProperty("protected") String protectedHeader,
                                 String signature) {

    public AgentCardSignature {
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

        public AgentCardSignature build() {
            return new AgentCardSignature(header, protectedHeader, signature);
        }
    }
}
