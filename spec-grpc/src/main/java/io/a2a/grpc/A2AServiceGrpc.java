package io.a2a.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * A2AService defines the gRPC version of the A2A protocol. This has a slightly
 * different shape than the JSONRPC version to better conform to AIP-127,
 * where appropriate. The nouns are AgentCard, Message, Task and
 * TaskPushNotificationConfig.
 * - Messages are not a standard resource so there is no get/delete/update/list
 *   interface, only a send and stream custom methods.
 * - Tasks have a get interface and custom cancel and subscribe methods.
 * - TaskPushNotificationConfig are a resource whose parent is a task.
 *   They have get, list and create methods.
 * - AgentCard is a static resource with only a get method.
 * fields are not present as they don't comply with AIP rules, and the
 * optional history_length on the get task method is not present as it also
 * violates AIP-127 and AIP-131.
 * </pre>
 */
// Temporarily comment this out until https://github.com/grpc/grpc-java/pull/12080 is included in a release
//@javax.annotation.Generated(
//    value = "by gRPC proto compiler (version 1.73.0)",
//    comments = "Source: a2a.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class A2AServiceGrpc {

  private A2AServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "a2a.v1.A2AService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.SendMessageRequest,
      io.a2a.grpc.SendMessageResponse> getSendMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendMessage",
      requestType = io.a2a.grpc.SendMessageRequest.class,
      responseType = io.a2a.grpc.SendMessageResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.SendMessageRequest,
      io.a2a.grpc.SendMessageResponse> getSendMessageMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.SendMessageRequest, io.a2a.grpc.SendMessageResponse> getSendMessageMethod;
    if ((getSendMessageMethod = A2AServiceGrpc.getSendMessageMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getSendMessageMethod = A2AServiceGrpc.getSendMessageMethod) == null) {
          A2AServiceGrpc.getSendMessageMethod = getSendMessageMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.SendMessageRequest, io.a2a.grpc.SendMessageResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.SendMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.SendMessageResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("SendMessage"))
              .build();
        }
      }
    }
    return getSendMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.SendMessageRequest,
      io.a2a.grpc.StreamResponse> getSendStreamingMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendStreamingMessage",
      requestType = io.a2a.grpc.SendMessageRequest.class,
      responseType = io.a2a.grpc.StreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.SendMessageRequest,
      io.a2a.grpc.StreamResponse> getSendStreamingMessageMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.SendMessageRequest, io.a2a.grpc.StreamResponse> getSendStreamingMessageMethod;
    if ((getSendStreamingMessageMethod = A2AServiceGrpc.getSendStreamingMessageMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getSendStreamingMessageMethod = A2AServiceGrpc.getSendStreamingMessageMethod) == null) {
          A2AServiceGrpc.getSendStreamingMessageMethod = getSendStreamingMessageMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.SendMessageRequest, io.a2a.grpc.StreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendStreamingMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.SendMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.StreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("SendStreamingMessage"))
              .build();
        }
      }
    }
    return getSendStreamingMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.GetTaskRequest,
      io.a2a.grpc.Task> getGetTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTask",
      requestType = io.a2a.grpc.GetTaskRequest.class,
      responseType = io.a2a.grpc.Task.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.GetTaskRequest,
      io.a2a.grpc.Task> getGetTaskMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.GetTaskRequest, io.a2a.grpc.Task> getGetTaskMethod;
    if ((getGetTaskMethod = A2AServiceGrpc.getGetTaskMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getGetTaskMethod = A2AServiceGrpc.getGetTaskMethod) == null) {
          A2AServiceGrpc.getGetTaskMethod = getGetTaskMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.GetTaskRequest, io.a2a.grpc.Task>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.GetTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.Task.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("GetTask"))
              .build();
        }
      }
    }
    return getGetTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.CancelTaskRequest,
      io.a2a.grpc.Task> getCancelTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CancelTask",
      requestType = io.a2a.grpc.CancelTaskRequest.class,
      responseType = io.a2a.grpc.Task.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.CancelTaskRequest,
      io.a2a.grpc.Task> getCancelTaskMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.CancelTaskRequest, io.a2a.grpc.Task> getCancelTaskMethod;
    if ((getCancelTaskMethod = A2AServiceGrpc.getCancelTaskMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getCancelTaskMethod = A2AServiceGrpc.getCancelTaskMethod) == null) {
          A2AServiceGrpc.getCancelTaskMethod = getCancelTaskMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.CancelTaskRequest, io.a2a.grpc.Task>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CancelTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.CancelTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.Task.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("CancelTask"))
              .build();
        }
      }
    }
    return getCancelTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.TaskSubscriptionRequest,
      io.a2a.grpc.StreamResponse> getTaskSubscriptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "TaskSubscription",
      requestType = io.a2a.grpc.TaskSubscriptionRequest.class,
      responseType = io.a2a.grpc.StreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.TaskSubscriptionRequest,
      io.a2a.grpc.StreamResponse> getTaskSubscriptionMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.TaskSubscriptionRequest, io.a2a.grpc.StreamResponse> getTaskSubscriptionMethod;
    if ((getTaskSubscriptionMethod = A2AServiceGrpc.getTaskSubscriptionMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getTaskSubscriptionMethod = A2AServiceGrpc.getTaskSubscriptionMethod) == null) {
          A2AServiceGrpc.getTaskSubscriptionMethod = getTaskSubscriptionMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.TaskSubscriptionRequest, io.a2a.grpc.StreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TaskSubscription"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.TaskSubscriptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.StreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("TaskSubscription"))
              .build();
        }
      }
    }
    return getTaskSubscriptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.CreateTaskPushNotificationConfigRequest,
      io.a2a.grpc.TaskPushNotificationConfig> getCreateTaskPushNotificationConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateTaskPushNotificationConfig",
      requestType = io.a2a.grpc.CreateTaskPushNotificationConfigRequest.class,
      responseType = io.a2a.grpc.TaskPushNotificationConfig.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.CreateTaskPushNotificationConfigRequest,
      io.a2a.grpc.TaskPushNotificationConfig> getCreateTaskPushNotificationConfigMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.CreateTaskPushNotificationConfigRequest, io.a2a.grpc.TaskPushNotificationConfig> getCreateTaskPushNotificationConfigMethod;
    if ((getCreateTaskPushNotificationConfigMethod = A2AServiceGrpc.getCreateTaskPushNotificationConfigMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getCreateTaskPushNotificationConfigMethod = A2AServiceGrpc.getCreateTaskPushNotificationConfigMethod) == null) {
          A2AServiceGrpc.getCreateTaskPushNotificationConfigMethod = getCreateTaskPushNotificationConfigMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.CreateTaskPushNotificationConfigRequest, io.a2a.grpc.TaskPushNotificationConfig>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateTaskPushNotificationConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.CreateTaskPushNotificationConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.TaskPushNotificationConfig.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("CreateTaskPushNotificationConfig"))
              .build();
        }
      }
    }
    return getCreateTaskPushNotificationConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.GetTaskPushNotificationConfigRequest,
      io.a2a.grpc.TaskPushNotificationConfig> getGetTaskPushNotificationConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTaskPushNotificationConfig",
      requestType = io.a2a.grpc.GetTaskPushNotificationConfigRequest.class,
      responseType = io.a2a.grpc.TaskPushNotificationConfig.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.GetTaskPushNotificationConfigRequest,
      io.a2a.grpc.TaskPushNotificationConfig> getGetTaskPushNotificationConfigMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.GetTaskPushNotificationConfigRequest, io.a2a.grpc.TaskPushNotificationConfig> getGetTaskPushNotificationConfigMethod;
    if ((getGetTaskPushNotificationConfigMethod = A2AServiceGrpc.getGetTaskPushNotificationConfigMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getGetTaskPushNotificationConfigMethod = A2AServiceGrpc.getGetTaskPushNotificationConfigMethod) == null) {
          A2AServiceGrpc.getGetTaskPushNotificationConfigMethod = getGetTaskPushNotificationConfigMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.GetTaskPushNotificationConfigRequest, io.a2a.grpc.TaskPushNotificationConfig>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTaskPushNotificationConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.GetTaskPushNotificationConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.TaskPushNotificationConfig.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("GetTaskPushNotificationConfig"))
              .build();
        }
      }
    }
    return getGetTaskPushNotificationConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.ListTaskPushNotificationConfigRequest,
      io.a2a.grpc.ListTaskPushNotificationConfigResponse> getListTaskPushNotificationConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListTaskPushNotificationConfig",
      requestType = io.a2a.grpc.ListTaskPushNotificationConfigRequest.class,
      responseType = io.a2a.grpc.ListTaskPushNotificationConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.ListTaskPushNotificationConfigRequest,
      io.a2a.grpc.ListTaskPushNotificationConfigResponse> getListTaskPushNotificationConfigMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.ListTaskPushNotificationConfigRequest, io.a2a.grpc.ListTaskPushNotificationConfigResponse> getListTaskPushNotificationConfigMethod;
    if ((getListTaskPushNotificationConfigMethod = A2AServiceGrpc.getListTaskPushNotificationConfigMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getListTaskPushNotificationConfigMethod = A2AServiceGrpc.getListTaskPushNotificationConfigMethod) == null) {
          A2AServiceGrpc.getListTaskPushNotificationConfigMethod = getListTaskPushNotificationConfigMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.ListTaskPushNotificationConfigRequest, io.a2a.grpc.ListTaskPushNotificationConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListTaskPushNotificationConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.ListTaskPushNotificationConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.ListTaskPushNotificationConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("ListTaskPushNotificationConfig"))
              .build();
        }
      }
    }
    return getListTaskPushNotificationConfigMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.GetAgentCardRequest,
      io.a2a.grpc.AgentCard> getGetAgentCardMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAgentCard",
      requestType = io.a2a.grpc.GetAgentCardRequest.class,
      responseType = io.a2a.grpc.AgentCard.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.GetAgentCardRequest,
      io.a2a.grpc.AgentCard> getGetAgentCardMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.GetAgentCardRequest, io.a2a.grpc.AgentCard> getGetAgentCardMethod;
    if ((getGetAgentCardMethod = A2AServiceGrpc.getGetAgentCardMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getGetAgentCardMethod = A2AServiceGrpc.getGetAgentCardMethod) == null) {
          A2AServiceGrpc.getGetAgentCardMethod = getGetAgentCardMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.GetAgentCardRequest, io.a2a.grpc.AgentCard>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAgentCard"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.GetAgentCardRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.AgentCard.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("GetAgentCard"))
              .build();
        }
      }
    }
    return getGetAgentCardMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.DeleteTaskPushNotificationConfigRequest,
      com.google.protobuf.Empty> getDeleteTaskPushNotificationConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteTaskPushNotificationConfig",
      requestType = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.DeleteTaskPushNotificationConfigRequest,
      com.google.protobuf.Empty> getDeleteTaskPushNotificationConfigMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.DeleteTaskPushNotificationConfigRequest, com.google.protobuf.Empty> getDeleteTaskPushNotificationConfigMethod;
    if ((getDeleteTaskPushNotificationConfigMethod = A2AServiceGrpc.getDeleteTaskPushNotificationConfigMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getDeleteTaskPushNotificationConfigMethod = A2AServiceGrpc.getDeleteTaskPushNotificationConfigMethod) == null) {
          A2AServiceGrpc.getDeleteTaskPushNotificationConfigMethod = getDeleteTaskPushNotificationConfigMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.DeleteTaskPushNotificationConfigRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteTaskPushNotificationConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("DeleteTaskPushNotificationConfig"))
              .build();
        }
      }
    }
    return getDeleteTaskPushNotificationConfigMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static A2AServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2AServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2AServiceStub>() {
        @java.lang.Override
        public A2AServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2AServiceStub(channel, callOptions);
        }
      };
    return A2AServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static A2AServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2AServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2AServiceBlockingV2Stub>() {
        @java.lang.Override
        public A2AServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2AServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return A2AServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static A2AServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2AServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2AServiceBlockingStub>() {
        @java.lang.Override
        public A2AServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2AServiceBlockingStub(channel, callOptions);
        }
      };
    return A2AServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static A2AServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<A2AServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<A2AServiceFutureStub>() {
        @java.lang.Override
        public A2AServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new A2AServiceFutureStub(channel, callOptions);
        }
      };
    return A2AServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * A2AService defines the gRPC version of the A2A protocol. This has a slightly
   * different shape than the JSONRPC version to better conform to AIP-127,
   * where appropriate. The nouns are AgentCard, Message, Task and
   * TaskPushNotificationConfig.
   * - Messages are not a standard resource so there is no get/delete/update/list
   *   interface, only a send and stream custom methods.
   * - Tasks have a get interface and custom cancel and subscribe methods.
   * - TaskPushNotificationConfig are a resource whose parent is a task.
   *   They have get, list and create methods.
   * - AgentCard is a static resource with only a get method.
   * fields are not present as they don't comply with AIP rules, and the
   * optional history_length on the get task method is not present as it also
   * violates AIP-127 and AIP-131.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Send a message to the agent. This is a blocking call that will return the
     * task once it is completed, or a LRO if requested.
     * </pre>
     */
    default void sendMessage(io.a2a.grpc.SendMessageRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendMessageMethod(), responseObserver);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming call that will return a stream of
     * task update events until the Task is in an interrupted or terminal state.
     * </pre>
     */
    default void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendStreamingMessageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the current state of a task from the agent.
     * </pre>
     */
    default void getTask(io.a2a.grpc.GetTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.Task> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * Cancel a task from the agent. If supported one should expect no
     * more task updates for the task.
     * </pre>
     */
    default void cancelTask(io.a2a.grpc.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.Task> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCancelTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * TaskSubscription is a streaming call that will return a stream of task
     * update events. This attaches the stream to an existing in process task.
     * If the task is complete the stream will return the completed task (like
     * GetTask) and close the stream.
     * </pre>
     */
    default void taskSubscription(io.a2a.grpc.TaskSubscriptionRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTaskSubscriptionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Set a push notification config for a task.
     * </pre>
     */
    default void createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateTaskPushNotificationConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a push notification config for a task.
     * </pre>
     */
    default void getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTaskPushNotificationConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a list of push notifications configured for a task.
     * </pre>
     */
    default void listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListTaskPushNotificationConfigMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetAgentCard returns the agent card for the agent.
     * </pre>
     */
    default void getAgentCard(io.a2a.grpc.GetAgentCardRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAgentCardMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a push notification config for a task.
     * </pre>
     */
    default void deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteTaskPushNotificationConfigMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service A2AService.
   * <pre>
   * A2AService defines the gRPC version of the A2A protocol. This has a slightly
   * different shape than the JSONRPC version to better conform to AIP-127,
   * where appropriate. The nouns are AgentCard, Message, Task and
   * TaskPushNotificationConfig.
   * - Messages are not a standard resource so there is no get/delete/update/list
   *   interface, only a send and stream custom methods.
   * - Tasks have a get interface and custom cancel and subscribe methods.
   * - TaskPushNotificationConfig are a resource whose parent is a task.
   *   They have get, list and create methods.
   * - AgentCard is a static resource with only a get method.
   * fields are not present as they don't comply with AIP rules, and the
   * optional history_length on the get task method is not present as it also
   * violates AIP-127 and AIP-131.
   * </pre>
   */
  public static abstract class A2AServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return A2AServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service A2AService.
   * <pre>
   * A2AService defines the gRPC version of the A2A protocol. This has a slightly
   * different shape than the JSONRPC version to better conform to AIP-127,
   * where appropriate. The nouns are AgentCard, Message, Task and
   * TaskPushNotificationConfig.
   * - Messages are not a standard resource so there is no get/delete/update/list
   *   interface, only a send and stream custom methods.
   * - Tasks have a get interface and custom cancel and subscribe methods.
   * - TaskPushNotificationConfig are a resource whose parent is a task.
   *   They have get, list and create methods.
   * - AgentCard is a static resource with only a get method.
   * fields are not present as they don't comply with AIP rules, and the
   * optional history_length on the get task method is not present as it also
   * violates AIP-127 and AIP-131.
   * </pre>
   */
  public static final class A2AServiceStub
      extends io.grpc.stub.AbstractAsyncStub<A2AServiceStub> {
    private A2AServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2AServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2AServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Send a message to the agent. This is a blocking call that will return the
     * task once it is completed, or a LRO if requested.
     * </pre>
     */
    public void sendMessage(io.a2a.grpc.SendMessageRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming call that will return a stream of
     * task update events until the Task is in an interrupted or terminal state.
     * </pre>
     */
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSendStreamingMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the current state of a task from the agent.
     * </pre>
     */
    public void getTask(io.a2a.grpc.GetTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.Task> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Cancel a task from the agent. If supported one should expect no
     * more task updates for the task.
     * </pre>
     */
    public void cancelTask(io.a2a.grpc.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.Task> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * TaskSubscription is a streaming call that will return a stream of task
     * update events. This attaches the stream to an existing in process task.
     * If the task is complete the stream will return the completed task (like
     * GetTask) and close the stream.
     * </pre>
     */
    public void taskSubscription(io.a2a.grpc.TaskSubscriptionRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getTaskSubscriptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Set a push notification config for a task.
     * </pre>
     */
    public void createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateTaskPushNotificationConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a push notification config for a task.
     * </pre>
     */
    public void getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetTaskPushNotificationConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a list of push notifications configured for a task.
     * </pre>
     */
    public void listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListTaskPushNotificationConfigMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetAgentCard returns the agent card for the agent.
     * </pre>
     */
    public void getAgentCard(io.a2a.grpc.GetAgentCardRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetAgentCardMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a push notification config for a task.
     * </pre>
     */
    public void deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteTaskPushNotificationConfigMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service A2AService.
   * <pre>
   * A2AService defines the gRPC version of the A2A protocol. This has a slightly
   * different shape than the JSONRPC version to better conform to AIP-127,
   * where appropriate. The nouns are AgentCard, Message, Task and
   * TaskPushNotificationConfig.
   * - Messages are not a standard resource so there is no get/delete/update/list
   *   interface, only a send and stream custom methods.
   * - Tasks have a get interface and custom cancel and subscribe methods.
   * - TaskPushNotificationConfig are a resource whose parent is a task.
   *   They have get, list and create methods.
   * - AgentCard is a static resource with only a get method.
   * fields are not present as they don't comply with AIP rules, and the
   * optional history_length on the get task method is not present as it also
   * violates AIP-127 and AIP-131.
   * </pre>
   */
  public static final class A2AServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<A2AServiceBlockingV2Stub> {
    private A2AServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2AServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2AServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Send a message to the agent. This is a blocking call that will return the
     * task once it is completed, or a LRO if requested.
     * </pre>
     */
    public io.a2a.grpc.SendMessageResponse sendMessage(io.a2a.grpc.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming call that will return a stream of
     * task update events until the Task is in an interrupted or terminal state.
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, io.a2a.grpc.StreamResponse>
        sendStreamingMessage(io.a2a.grpc.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getSendStreamingMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the current state of a task from the agent.
     * </pre>
     */
    public io.a2a.grpc.Task getTask(io.a2a.grpc.GetTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Cancel a task from the agent. If supported one should expect no
     * more task updates for the task.
     * </pre>
     */
    public io.a2a.grpc.Task cancelTask(io.a2a.grpc.CancelTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCancelTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * TaskSubscription is a streaming call that will return a stream of task
     * update events. This attaches the stream to an existing in process task.
     * If the task is complete the stream will return the completed task (like
     * GetTask) and close the stream.
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, io.a2a.grpc.StreamResponse>
        taskSubscription(io.a2a.grpc.TaskSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getTaskSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set a push notification config for a task.
     * </pre>
     */
    public io.a2a.grpc.TaskPushNotificationConfig createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a push notification config for a task.
     * </pre>
     */
    public io.a2a.grpc.TaskPushNotificationConfig getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of push notifications configured for a task.
     * </pre>
     */
    public io.a2a.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAgentCard returns the agent card for the agent.
     * </pre>
     */
    public io.a2a.grpc.AgentCard getAgentCard(io.a2a.grpc.GetAgentCardRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAgentCardMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a push notification config for a task.
     * </pre>
     */
    public com.google.protobuf.Empty deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service A2AService.
   * <pre>
   * A2AService defines the gRPC version of the A2A protocol. This has a slightly
   * different shape than the JSONRPC version to better conform to AIP-127,
   * where appropriate. The nouns are AgentCard, Message, Task and
   * TaskPushNotificationConfig.
   * - Messages are not a standard resource so there is no get/delete/update/list
   *   interface, only a send and stream custom methods.
   * - Tasks have a get interface and custom cancel and subscribe methods.
   * - TaskPushNotificationConfig are a resource whose parent is a task.
   *   They have get, list and create methods.
   * - AgentCard is a static resource with only a get method.
   * fields are not present as they don't comply with AIP rules, and the
   * optional history_length on the get task method is not present as it also
   * violates AIP-127 and AIP-131.
   * </pre>
   */
  public static final class A2AServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<A2AServiceBlockingStub> {
    private A2AServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2AServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2AServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Send a message to the agent. This is a blocking call that will return the
     * task once it is completed, or a LRO if requested.
     * </pre>
     */
    public io.a2a.grpc.SendMessageResponse sendMessage(io.a2a.grpc.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming call that will return a stream of
     * task update events until the Task is in an interrupted or terminal state.
     * </pre>
     */
    public java.util.Iterator<io.a2a.grpc.StreamResponse> sendStreamingMessage(
        io.a2a.grpc.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSendStreamingMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the current state of a task from the agent.
     * </pre>
     */
    public io.a2a.grpc.Task getTask(io.a2a.grpc.GetTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Cancel a task from the agent. If supported one should expect no
     * more task updates for the task.
     * </pre>
     */
    public io.a2a.grpc.Task cancelTask(io.a2a.grpc.CancelTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCancelTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * TaskSubscription is a streaming call that will return a stream of task
     * update events. This attaches the stream to an existing in process task.
     * If the task is complete the stream will return the completed task (like
     * GetTask) and close the stream.
     * </pre>
     */
    public java.util.Iterator<io.a2a.grpc.StreamResponse> taskSubscription(
        io.a2a.grpc.TaskSubscriptionRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getTaskSubscriptionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Set a push notification config for a task.
     * </pre>
     */
    public io.a2a.grpc.TaskPushNotificationConfig createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a push notification config for a task.
     * </pre>
     */
    public io.a2a.grpc.TaskPushNotificationConfig getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of push notifications configured for a task.
     * </pre>
     */
    public io.a2a.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAgentCard returns the agent card for the agent.
     * </pre>
     */
    public io.a2a.grpc.AgentCard getAgentCard(io.a2a.grpc.GetAgentCardRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetAgentCardMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a push notification config for a task.
     * </pre>
     */
    public com.google.protobuf.Empty deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service A2AService.
   * <pre>
   * A2AService defines the gRPC version of the A2A protocol. This has a slightly
   * different shape than the JSONRPC version to better conform to AIP-127,
   * where appropriate. The nouns are AgentCard, Message, Task and
   * TaskPushNotificationConfig.
   * - Messages are not a standard resource so there is no get/delete/update/list
   *   interface, only a send and stream custom methods.
   * - Tasks have a get interface and custom cancel and subscribe methods.
   * - TaskPushNotificationConfig are a resource whose parent is a task.
   *   They have get, list and create methods.
   * - AgentCard is a static resource with only a get method.
   * fields are not present as they don't comply with AIP rules, and the
   * optional history_length on the get task method is not present as it also
   * violates AIP-127 and AIP-131.
   * </pre>
   */
  public static final class A2AServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<A2AServiceFutureStub> {
    private A2AServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected A2AServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new A2AServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Send a message to the agent. This is a blocking call that will return the
     * task once it is completed, or a LRO if requested.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.SendMessageResponse> sendMessage(
        io.a2a.grpc.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the current state of a task from the agent.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.Task> getTask(
        io.a2a.grpc.GetTaskRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Cancel a task from the agent. If supported one should expect no
     * more task updates for the task.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.Task> cancelTask(
        io.a2a.grpc.CancelTaskRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Set a push notification config for a task.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.TaskPushNotificationConfig> createTaskPushNotificationConfig(
        io.a2a.grpc.CreateTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateTaskPushNotificationConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a push notification config for a task.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.TaskPushNotificationConfig> getTaskPushNotificationConfig(
        io.a2a.grpc.GetTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetTaskPushNotificationConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a list of push notifications configured for a task.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.ListTaskPushNotificationConfigResponse> listTaskPushNotificationConfig(
        io.a2a.grpc.ListTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListTaskPushNotificationConfigMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetAgentCard returns the agent card for the agent.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.AgentCard> getAgentCard(
        io.a2a.grpc.GetAgentCardRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetAgentCardMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a push notification config for a task.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> deleteTaskPushNotificationConfig(
        io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteTaskPushNotificationConfigMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_MESSAGE = 0;
  private static final int METHODID_SEND_STREAMING_MESSAGE = 1;
  private static final int METHODID_GET_TASK = 2;
  private static final int METHODID_CANCEL_TASK = 3;
  private static final int METHODID_TASK_SUBSCRIPTION = 4;
  private static final int METHODID_CREATE_TASK_PUSH_NOTIFICATION_CONFIG = 5;
  private static final int METHODID_GET_TASK_PUSH_NOTIFICATION_CONFIG = 6;
  private static final int METHODID_LIST_TASK_PUSH_NOTIFICATION_CONFIG = 7;
  private static final int METHODID_GET_AGENT_CARD = 8;
  private static final int METHODID_DELETE_TASK_PUSH_NOTIFICATION_CONFIG = 9;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_MESSAGE:
          serviceImpl.sendMessage((io.a2a.grpc.SendMessageRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.SendMessageResponse>) responseObserver);
          break;
        case METHODID_SEND_STREAMING_MESSAGE:
          serviceImpl.sendStreamingMessage((io.a2a.grpc.SendMessageRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse>) responseObserver);
          break;
        case METHODID_GET_TASK:
          serviceImpl.getTask((io.a2a.grpc.GetTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.Task>) responseObserver);
          break;
        case METHODID_CANCEL_TASK:
          serviceImpl.cancelTask((io.a2a.grpc.CancelTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.Task>) responseObserver);
          break;
        case METHODID_TASK_SUBSCRIPTION:
          serviceImpl.taskSubscription((io.a2a.grpc.TaskSubscriptionRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse>) responseObserver);
          break;
        case METHODID_CREATE_TASK_PUSH_NOTIFICATION_CONFIG:
          serviceImpl.createTaskPushNotificationConfig((io.a2a.grpc.CreateTaskPushNotificationConfigRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.TaskPushNotificationConfig>) responseObserver);
          break;
        case METHODID_GET_TASK_PUSH_NOTIFICATION_CONFIG:
          serviceImpl.getTaskPushNotificationConfig((io.a2a.grpc.GetTaskPushNotificationConfigRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.TaskPushNotificationConfig>) responseObserver);
          break;
        case METHODID_LIST_TASK_PUSH_NOTIFICATION_CONFIG:
          serviceImpl.listTaskPushNotificationConfig((io.a2a.grpc.ListTaskPushNotificationConfigRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse>) responseObserver);
          break;
        case METHODID_GET_AGENT_CARD:
          serviceImpl.getAgentCard((io.a2a.grpc.GetAgentCardRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.AgentCard>) responseObserver);
          break;
        case METHODID_DELETE_TASK_PUSH_NOTIFICATION_CONFIG:
          serviceImpl.deleteTaskPushNotificationConfig((io.a2a.grpc.DeleteTaskPushNotificationConfigRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.SendMessageRequest,
              io.a2a.grpc.SendMessageResponse>(
                service, METHODID_SEND_MESSAGE)))
        .addMethod(
          getSendStreamingMessageMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.a2a.grpc.SendMessageRequest,
              io.a2a.grpc.StreamResponse>(
                service, METHODID_SEND_STREAMING_MESSAGE)))
        .addMethod(
          getGetTaskMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.GetTaskRequest,
              io.a2a.grpc.Task>(
                service, METHODID_GET_TASK)))
        .addMethod(
          getCancelTaskMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.CancelTaskRequest,
              io.a2a.grpc.Task>(
                service, METHODID_CANCEL_TASK)))
        .addMethod(
          getTaskSubscriptionMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.a2a.grpc.TaskSubscriptionRequest,
              io.a2a.grpc.StreamResponse>(
                service, METHODID_TASK_SUBSCRIPTION)))
        .addMethod(
          getCreateTaskPushNotificationConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.CreateTaskPushNotificationConfigRequest,
              io.a2a.grpc.TaskPushNotificationConfig>(
                service, METHODID_CREATE_TASK_PUSH_NOTIFICATION_CONFIG)))
        .addMethod(
          getGetTaskPushNotificationConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.GetTaskPushNotificationConfigRequest,
              io.a2a.grpc.TaskPushNotificationConfig>(
                service, METHODID_GET_TASK_PUSH_NOTIFICATION_CONFIG)))
        .addMethod(
          getListTaskPushNotificationConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.ListTaskPushNotificationConfigRequest,
              io.a2a.grpc.ListTaskPushNotificationConfigResponse>(
                service, METHODID_LIST_TASK_PUSH_NOTIFICATION_CONFIG)))
        .addMethod(
          getGetAgentCardMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.GetAgentCardRequest,
              io.a2a.grpc.AgentCard>(
                service, METHODID_GET_AGENT_CARD)))
        .addMethod(
          getDeleteTaskPushNotificationConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.DeleteTaskPushNotificationConfigRequest,
              com.google.protobuf.Empty>(
                service, METHODID_DELETE_TASK_PUSH_NOTIFICATION_CONFIG)))
        .build();
  }

  private static abstract class A2AServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    A2AServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.a2a.grpc.A2A.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("A2AService");
    }
  }

  private static final class A2AServiceFileDescriptorSupplier
      extends A2AServiceBaseDescriptorSupplier {
    A2AServiceFileDescriptorSupplier() {}
  }

  private static final class A2AServiceMethodDescriptorSupplier
      extends A2AServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    A2AServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (A2AServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new A2AServiceFileDescriptorSupplier())
              .addMethod(getSendMessageMethod())
              .addMethod(getSendStreamingMessageMethod())
              .addMethod(getGetTaskMethod())
              .addMethod(getCancelTaskMethod())
              .addMethod(getTaskSubscriptionMethod())
              .addMethod(getCreateTaskPushNotificationConfigMethod())
              .addMethod(getGetTaskPushNotificationConfigMethod())
              .addMethod(getListTaskPushNotificationConfigMethod())
              .addMethod(getGetAgentCardMethod())
              .addMethod(getDeleteTaskPushNotificationConfigMethod())
              .build();
        }
      }
    }
    return result;
  }
}
