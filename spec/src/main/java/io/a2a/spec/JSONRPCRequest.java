package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Abstract base class for JSON-RPC 2.0 request messages.
 * 
 * This sealed class represents a JSON-RPC request according to the JSON-RPC 2.0
 * specification. A request contains:
 * - "jsonrpc": the protocol version ("2.0")
 * - "method": the name of the method to be invoked
 * - "params": the parameter values (optional)
 * - "id": the request identifier for correlation with responses (optional for notifications)
 * 
 * This class is sealed to ensure type safety and restrict implementations to
 * the permitted subtypes: {@link NonStreamingJSONRPCRequest} and {@link StreamingJSONRPCRequest}.
 * 
 * @param <T> the type of the parameters object
 * @see NonStreamingJSONRPCRequest
 * @see StreamingJSONRPCRequest
 * @see JSONRPCMessage
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract sealed class JSONRPCRequest<T> implements JSONRPCMessage permits NonStreamingJSONRPCRequest, StreamingJSONRPCRequest {

    /** The JSON-RPC protocol version */
    protected String jsonrpc;
    
    /** The request identifier for correlation with responses */
    protected Object id;
    
    /** The name of the method to be invoked */
    protected String method;
    
    /** The parameter values for the method invocation */
    protected T params;

    /**
     * Default constructor for JSON deserialization.
     */
    public JSONRPCRequest() {
    }

    /**
     * Constructs a new JSON-RPC request with the specified parameters.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must not be null)
     * @param id the request identifier for correlation with responses (can be null for notifications)
     * @param method the name of the method to be invoked (must not be null)
     * @param params the parameter values for the method invocation (can be null)
     * @throws IllegalArgumentException if jsonrpc or method is null, or if id is not null, string, or integer
     */
    public JSONRPCRequest(String jsonrpc, Object id, String method, T params) {
        Assert.checkNotNullParam("jsonrpc", jsonrpc);
        Assert.checkNotNullParam("method", method);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Returns the JSON-RPC protocol version.
     * 
     * @return the JSON-RPC protocol version (typically "2.0")
     */
    @Override
    public String getJsonrpc() {
        return this.jsonrpc;
    }

    /**
     * Returns the request identifier for correlation with responses.
     * 
     * @return the request identifier, or null for notification requests
     */
    @Override
    public Object getId() {
        return this.id;
    }

    /**
     * Returns the name of the method to be invoked.
     * 
     * @return the method name
     */
    public String getMethod() {
        return this.method;
    }

    /**
     * Returns the parameter values for the method invocation.
     * 
     * @return the parameters object, or null if no parameters are provided
     */
    public T getParams() {
        return this.params;
    }
}
