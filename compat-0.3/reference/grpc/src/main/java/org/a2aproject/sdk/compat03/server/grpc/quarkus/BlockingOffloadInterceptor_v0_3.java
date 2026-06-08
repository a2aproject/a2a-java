package org.a2aproject.sdk.compat03.server.grpc.quarkus;

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
 * v0.3 variant of {@code BlockingOffloadInterceptor} in the reference/grpc module.
 */
@ApplicationScoped
public class BlockingOffloadInterceptor_v0_3 implements ServerInterceptor {

    private final Executor executor;

    @Inject
    public BlockingOffloadInterceptor_v0_3(@Internal Executor executor) {
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
