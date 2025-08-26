package io.a2a.transport.grpc.handler;

import io.a2a.server.ServerCallContext;
import io.grpc.stub.StreamObserver;

public interface CallContextFactory {
    <V> ServerCallContext create(StreamObserver<V> responseObserver);
}