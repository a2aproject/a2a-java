package org.a2aproject.sdk.server.grpc.quarkus;

import java.util.concurrent.Executor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.util.async.Internal;
import io.grpc.Context;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * gRPC server interceptor that offloads handler execution from the Vert.x event loop
 * to a worker thread, using a forked gRPC {@link Context}.
 *
 * <p>In Quarkus separate-server mode ({@code quarkus.grpc.server.use-separate-server=true}),
 * the {@code @Blocking} annotation is ignored and gRPC handlers run on the Vert.x event loop.
 * Synchronous operations like {@code sendMessage()} deadlock the event loop. This interceptor
 * wraps the {@code onHalfClose()} callback to run the handler on a worker thread instead.
 *
 * <p>The context is forked ({@link Context#fork()}) so that the handler's outbound gRPC calls
 * do not inherit the inbound call's cancellation signal. Without this, Quarkus'
 * {@code ContextStorageOverride} propagates the server context through the
 * {@code ManagedExecutor}, causing outbound client calls to be cancelled when the
 * inbound caller disconnects.
 *
 * <p>This applies to both unary and server-streaming methods (which both have a single
 * inbound request). Client-streaming and bidi-streaming methods are excluded.
 */
@ApplicationScoped
public class BlockingOffloadInterceptor implements ServerInterceptor {

    private final Executor executor;

    @Inject
    public BlockingOffloadInterceptor(@Internal Executor executor) {
        this.executor = executor;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        MethodDescriptor.MethodType type = call.getMethodDescriptor().getType();
        if (type == MethodDescriptor.MethodType.CLIENT_STREAMING
                || type == MethodDescriptor.MethodType.BIDI_STREAMING) {
            return next.startCall(call, headers);
        }

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new SimpleForwardingServerCallListener<ReqT>(delegate) {
            @Override
            public void onHalfClose() {
                Context grpcContext = Context.current().fork();
                try {
                    executor.execute(() -> {
                        Context previous = grpcContext.attach();
                        try {
                            super.onHalfClose();
                        } finally {
                            grpcContext.detach(previous);
                        }
                    });
                } catch (Exception e) {
                    call.close(Status.INTERNAL.withDescription("Failed to offload to worker thread: " + e.getMessage()), new Metadata());
                }
            }
        };
    }
}
