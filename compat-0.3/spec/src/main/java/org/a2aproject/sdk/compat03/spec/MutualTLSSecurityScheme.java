package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.spec.MutualTLSSecurityScheme.MUTUAL_TLS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.a2aproject.sdk.util.Assert;

/**
 * Defines a security scheme using mTLS authentication.
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
