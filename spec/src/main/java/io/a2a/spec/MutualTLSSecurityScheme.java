package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.MutualTLSSecurityScheme.MUTUAL_TLS;

/**
 * Mutual TLS (mTLS) security scheme for agent authentication.
 * <p>
 * This security scheme uses mutual TLS authentication, where both the client and server
 * present X.509 certificates to authenticate each other. This provides strong,
 * certificate-based authentication at the transport layer.
 * <p>
 * Unlike other authentication schemes that operate at the HTTP layer, mTLS authentication
 * occurs during the TLS handshake before any HTTP communication begins. This makes it
 * particularly suitable for high-security environments and machine-to-machine communication.
 * <p>
 * Example usage:
 * <pre>{@code
 * MutualTLSSecurityScheme scheme = new MutualTLSSecurityScheme(
 *     "Client certificate authentication required"
 * );
 * }</pre>
 *
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8446#section-4.4.2">RFC 8446 - TLS 1.3 Client Authentication</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonTypeName(MUTUAL_TLS)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MutualTLSSecurityScheme implements SecurityScheme {

    public static final String MUTUAL_TLS = "mutualTLS";
    private final String description;
    private final String type;

    public MutualTLSSecurityScheme(String description) {
        this(description, MUTUAL_TLS);
    }

    public MutualTLSSecurityScheme() {
        this(null, MUTUAL_TLS);
    }

    @JsonCreator
    public MutualTLSSecurityScheme(@JsonProperty("description") String description,
                                   @JsonProperty("type") String type) {
        Assert.checkNotNullParam("type", type);
        if (!type.equals(MUTUAL_TLS)) {
            throw new IllegalArgumentException("Invalid type for MutualTLSSecurityScheme");
        }
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

}
