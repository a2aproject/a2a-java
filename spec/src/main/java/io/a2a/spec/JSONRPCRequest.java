package io.a2a.spec;

import io.a2a.util.Assert;
import io.a2a.util.Utils;

/**
 * Base class for JSON-RPC 2.0 requests in the A2A Protocol.
 * <p>
 * This sealed class represents a JSON-RPC 2.0 request message with parameterized support
 * for different request parameter types. It enforces the JSON-RPC 2.0 specification
 * requirements while providing type safety through generic parameters.
 * <p>
 * A JSON-RPC request consists of:
 * <ul>
 *   <li>{@code jsonrpc} - The protocol version (always "2.0")</li>
 *   <li>{@code method} - The name of the method to invoke</li>
 *   <li>{@code params} - Method parameters (optional, type varies by method)</li>
 *   <li>{@code id} - Correlation identifier for matching with responses (optional for notifications)</li>
 * </ul>
 * <p>
 * This class is sealed to ensure only {@link NonStreamingJSONRPCRequest} and
 * {@link StreamingJSONRPCRequest} can extend it, providing compile-time guarantees
 * about request types.
 *
  * @param <T> the type of the params object for this request
  * @see NonStreamingJSONRPCRequest
  * @see StreamingJSONRPCRequest
  * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
  * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
  */
 public abstract sealed class JSONRPCRequest<T> implements JSONRPCMessage permits NonStreamingJSONRPCRequest, StreamingJSONRPCRequest {
 
     /** The JSON-RPC protocol version. */
     protected String jsonrpc;
     /** The request identifier. */
     protected Object id;
     /** The method name to invoke. */
     protected String method;
     /** The method parameters. */
     protected T params;
 
     /**
      * Default constructor for JSON deserialization.
      */
     public JSONRPCRequest() {
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public String getJsonrpc() {
         return this.jsonrpc;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public Object getId() {
         return this.id;
     }
 
     /**
      * Gets the name of the method to be invoked.
      *
      * @return the method name
      */
     public String getMethod() {
         return this.method;
     }
 
     /**
      * Gets the parameters to be passed to the method.
      *
      * @return the method parameters, or null if none
      */
     public T getParams() {
         return this.params;
     }
 
     /**
      * Validates and sets JSON-RPC parameters.
      *
      * @param jsonrpc the JSON-RPC version
      * @param method the method name
      * @param id the request ID
      * @param params the parameters
      * @param paramsIsRequired whether parameters are required
      */
     protected void validateAndSetJsonParameters(String jsonrpc, String method, Object id, T params, boolean paramsIsRequired) {
         this.jsonrpc = Utils.defaultIfNull(jsonrpc, JSONRPC_VERSION);
         if (!JSONRPC_VERSION.equals(this.jsonrpc)) {
             throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
         }
         Assert.checkNotNullParam("method", method);
         this.method = method;
         Assert.isNullOrStringOrInteger(id);
         this.id = id;
         if (paramsIsRequired) {
             Assert.checkNotNullParam("params", params);
         }
         this.params = params;
     }
 }
