package org.a2aproject.sdk.itk;

import jakarta.enterprise.context.ApplicationScoped;

import org.jspecify.annotations.Nullable;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;

/**
 * Applies the ACTS credential rule to every gRPC call.
 *
 * <p>Global rather than per-service so the v1.0 and v0.3 services are covered alike: with
 * {@code ITK_ACTS_AUTH} set the card declares an agent-wide requirement, and a binding that served
 * anyone would make that claim false. See {@link ActsAuth} for why enforcement is opt-in at all.
 */
@GlobalInterceptor
@ApplicationScoped
public class ActsAuthInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
        @Nullable Status rejection = ActsAuth.grpcRejection(metadata);
        if (rejection != null) {
            call.close(rejection, new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
        return next.startCall(call, metadata);
    }
}
