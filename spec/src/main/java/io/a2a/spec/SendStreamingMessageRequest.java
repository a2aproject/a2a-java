package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

import java.util.UUID;

/**
 * JSON-RPC request to initiate a new task with streaming event delivery.
 * <p>
 * This request starts agent processing for a message and immediately returns a stream of events
 * representing the agent's progress and responses. Unlike {@link SendMessageRequest}, this enables
 * real-time progress updates as the agent processes the request.
 * <p>
 * The streaming response provides {@link StreamingEventKind} events such as task updates,
 * artifact additions, and status changes as they occur, rather than waiting for final completion.
 * <p>
 * This class implements the JSON-RPC {@code message/stream} method as specified in the A2A Protocol.
 *
 * @see SendMessageRequest for non-streaming message delivery
 * @see MessageSendParams for parameter structure
 * @see StreamingEventKind for event types in streaming responses
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendStreamingMessageRequest extends StreamingJSONRPCRequest<MessageSendParams> {

    public static final String METHOD = "SendStreamingMessage";

    @JsonCreator
    public SendStreamingMessageRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                       @JsonProperty("method") String method, @JsonProperty("params") MessageSendParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SendStreamingMessageRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public SendStreamingMessageRequest(Object id,  MessageSendParams params) {
        this(null, id, METHOD, params);
    }

    /**
     * Builder for constructing {@link SendStreamingMessageRequest} instances.
     * <p>
     * Provides a fluent API for building streaming message requests with optional customization
     * of JSON-RPC protocol fields.
     */
    public static class Builder {
            private String jsonrpc;
            private Object id;
            private String method = METHOD;
            private MessageSendParams params;

            /**
             * Sets the JSON-RPC protocol version.
             *
             * @param jsonrpc the JSON-RPC version (defaults to "2.0")
             * @return this builder
             */
            public Builder jsonrpc(String jsonrpc) {
                this.jsonrpc = jsonrpc;
                return this;
            }

            /**
             * Sets the JSON-RPC request ID.
             *
             * @param id the request identifier (auto-generated UUID if null)
             * @return this builder
             */
            public Builder id(Object id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the JSON-RPC method name.
             *
             * @param method the method name (defaults to "SendStreamingMessage")
             * @return this builder
             */
            public Builder method(String method) {
                this.method = method;
                return this;
            }

            /**
             * Sets the message send parameters.
             *
             * @param params the parameters containing the message and configuration
             * @return this builder
             */
            public Builder params(MessageSendParams params) {
                this.params = params;
                return this;
            }

            /**
             * Builds the {@link SendStreamingMessageRequest}.
             *
             * @return a new streaming message request instance
             */
            public SendStreamingMessageRequest build() {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                return new SendStreamingMessageRequest(jsonrpc, id, method, params);
            }
        }
    }
