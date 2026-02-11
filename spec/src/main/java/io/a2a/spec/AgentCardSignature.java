package io.a2a.spec;

import java.util.Map;

import com.google.gson.annotations.SerializedName;
import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a digital signature for an {@link AgentCard} using JSON Web Signature (JWS) format.
 * <p>
 * AgentCardSignature provides cryptographic proof that an AgentCard was issued by a trusted
 * authority and has not been tampered with. This enables verification of agent authenticity
 * and integrity in security-sensitive scenarios.
 * <p>
 * The signature follows RFC 7515 JSON Web Signature (JWS) specification, consisting of:
 * <ul>
 *   <li>A protected header (Base64URL-encoded JSON) containing algorithm and key information</li>
 *   <li>An optional unprotected header with additional metadata</li>
 *   <li>The signature value itself</li>
 * </ul>
 * <p>
 * Multiple signatures can be included in an AgentCard to support different verification
 * authorities or key algorithms.
 * <p>
 * This class is immutable. Use the {@link Builder} for construction.
 *
 * @param header optional unprotected header with additional metadata (optional)
 * @param protectedHeader Base64URL-encoded protected header containing algorithm and key info (required)
 * @param signature the Base64URL-encoded signature value (required)
 * @see AgentCard
 * @see <a href="https://tools.ietf.org/html/rfc7515">RFC 7515 - JSON Web Signature</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentCardSignature(@Nullable Map<String, Object> header, @SerializedName("protected")String protectedHeader,
                                 String signature) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param header the header parameter (see class-level JavaDoc)
     * @param protectedHeader the protectedHeader parameter (see class-level JavaDoc)
     * @param signature the signature parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if protectedHeader or signature is null
     */
    public AgentCardSignature {
        Assert.checkNotNullParam("protectedHeader", protectedHeader);
        Assert.checkNotNullParam("signature", signature);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link AgentCardSignature} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * AgentCardSignature sig = AgentCardSignature.builder()
     *     .protectedHeader("eyJhbGciOiJFUzI1NiJ9")
     *     .signature("DtEhU3ljbEg8L38VWAfUAqOyKAM6...")
     *     .header(Map.of("kid", "2024-01"))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private @Nullable Map<String, Object> header;
        @Nullable String protectedHeader;
        @Nullable String signature;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the optional unprotected header with additional metadata.
         *
         * @param header map of header parameters (optional)
         * @return this builder for method chaining
         */
        public Builder header(Map<String, Object> header) {
            this.header = header;
            return this;
        }

        /**
         * Sets the Base64URL-encoded protected header.
         * <p>
         * The protected header typically contains the algorithm ("alg") and may include
         * key identification ("kid") or other parameters that need integrity protection.
         *
         * @param protectedHeader the Base64URL-encoded protected header (required)
         * @return this builder for method chaining
         */
        public Builder protectedHeader(String protectedHeader) {
            this.protectedHeader = protectedHeader;
            return this;
        }

        /**
         * Sets the Base64URL-encoded signature value.
         *
         * @param signature the Base64URL-encoded signature (required)
         * @return this builder for method chaining
         */
        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        /**
         * Builds a new immutable {@link AgentCardSignature} from the current builder state.
         *
         * @return a new AgentCardSignature instance
         * @throws IllegalArgumentException if protectedHeader or signature is null
         */
        public AgentCardSignature build() {
            return new AgentCardSignature(
                    header,
                    Assert.checkNotNullParam("protectedHeader", protectedHeader),
                    Assert.checkNotNullParam("signature", signature));
        }
    }
}
