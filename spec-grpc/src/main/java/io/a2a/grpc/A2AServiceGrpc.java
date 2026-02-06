package io.a2a.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * A2AService defines the operations of the A2A protocol.
 * </pre>
 */
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

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.ListTasksRequest,
      io.a2a.grpc.ListTasksResponse> getListTasksMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListTasks",
      requestType = io.a2a.grpc.ListTasksRequest.class,
      responseType = io.a2a.grpc.ListTasksResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.ListTasksRequest,
      io.a2a.grpc.ListTasksResponse> getListTasksMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.ListTasksRequest, io.a2a.grpc.ListTasksResponse> getListTasksMethod;
    if ((getListTasksMethod = A2AServiceGrpc.getListTasksMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getListTasksMethod = A2AServiceGrpc.getListTasksMethod) == null) {
          A2AServiceGrpc.getListTasksMethod = getListTasksMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.ListTasksRequest, io.a2a.grpc.ListTasksResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListTasks"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.ListTasksRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.ListTasksResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("ListTasks"))
              .build();
        }
      }
    }
    return getListTasksMethod;
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

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.SubscribeToTaskRequest,
      io.a2a.grpc.StreamResponse> getSubscribeToTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubscribeToTask",
      requestType = io.a2a.grpc.SubscribeToTaskRequest.class,
      responseType = io.a2a.grpc.StreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.SubscribeToTaskRequest,
      io.a2a.grpc.StreamResponse> getSubscribeToTaskMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.SubscribeToTaskRequest, io.a2a.grpc.StreamResponse> getSubscribeToTaskMethod;
    if ((getSubscribeToTaskMethod = A2AServiceGrpc.getSubscribeToTaskMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getSubscribeToTaskMethod = A2AServiceGrpc.getSubscribeToTaskMethod) == null) {
          A2AServiceGrpc.getSubscribeToTaskMethod = getSubscribeToTaskMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.SubscribeToTaskRequest, io.a2a.grpc.StreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubscribeToTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.SubscribeToTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.StreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("SubscribeToTask"))
              .build();
        }
      }
    }
    return getSubscribeToTaskMethod;
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

  private static volatile io.grpc.MethodDescriptor<io.a2a.grpc.GetExtendedAgentCardRequest,
      io.a2a.grpc.AgentCard> getGetExtendedAgentCardMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetExtendedAgentCard",
      requestType = io.a2a.grpc.GetExtendedAgentCardRequest.class,
      responseType = io.a2a.grpc.AgentCard.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.a2a.grpc.GetExtendedAgentCardRequest,
      io.a2a.grpc.AgentCard> getGetExtendedAgentCardMethod() {
    io.grpc.MethodDescriptor<io.a2a.grpc.GetExtendedAgentCardRequest, io.a2a.grpc.AgentCard> getGetExtendedAgentCardMethod;
    if ((getGetExtendedAgentCardMethod = A2AServiceGrpc.getGetExtendedAgentCardMethod) == null) {
      synchronized (A2AServiceGrpc.class) {
        if ((getGetExtendedAgentCardMethod = A2AServiceGrpc.getGetExtendedAgentCardMethod) == null) {
          A2AServiceGrpc.getGetExtendedAgentCardMethod = getGetExtendedAgentCardMethod =
              io.grpc.MethodDescriptor.<io.a2a.grpc.GetExtendedAgentCardRequest, io.a2a.grpc.AgentCard>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetExtendedAgentCard"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.GetExtendedAgentCardRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.a2a.grpc.AgentCard.getDefaultInstance()))
              .setSchemaDescriptor(new A2AServiceMethodDescriptorSupplier("GetExtendedAgentCard"))
              .build();
        }
      }
    }
    return getGetExtendedAgentCardMethod;
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
   * A2AService defines the operations of the A2A protocol.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Send a message to the agent.
     * </pre>
     */
    default void sendMessage(io.a2a.grpc.SendMessageRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendMessageMethod(), responseObserver);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming version of SendMessage.
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
     * List tasks with optional filtering and pagination.
     * </pre>
     */
    default void listTasks(io.a2a.grpc.ListTasksRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.ListTasksResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListTasksMethod(), responseObserver);
    }

    /**
     * <pre>
     * Cancel a task.
     * </pre>
     */
    default void cancelTask(io.a2a.grpc.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.Task> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCancelTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * SubscribeToTask allows subscribing to task updates for tasks not in terminal state.
     * Returns UnsupportedOperationError if task is in terminal state (completed, failed, canceled, rejected).
     * </pre>
     */
    default void subscribeToTask(io.a2a.grpc.SubscribeToTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubscribeToTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a push notification config for a task.
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
     * GetExtendedAgentCard returns the extended agent card for authenticated agents.
     * </pre>
     */
    default void getExtendedAgentCard(io.a2a.grpc.GetExtendedAgentCardRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetExtendedAgentCardMethod(), responseObserver);
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
   * A2AService defines the operations of the A2A protocol.
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
   * A2AService defines the operations of the A2A protocol.
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
     * Send a message to the agent.
     * </pre>
     */
    public void sendMessage(io.a2a.grpc.SendMessageRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming version of SendMessage.
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
     * List tasks with optional filtering and pagination.
     * </pre>
     */
    public void listTasks(io.a2a.grpc.ListTasksRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.ListTasksResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListTasksMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Cancel a task.
     * </pre>
     */
    public void cancelTask(io.a2a.grpc.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.Task> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * SubscribeToTask allows subscribing to task updates for tasks not in terminal state.
     * Returns UnsupportedOperationError if task is in terminal state (completed, failed, canceled, rejected).
     * </pre>
     */
    public void subscribeToTask(io.a2a.grpc.SubscribeToTaskRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSubscribeToTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a push notification config for a task.
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
     * GetExtendedAgentCard returns the extended agent card for authenticated agents.
     * </pre>
     */
    public void getExtendedAgentCard(io.a2a.grpc.GetExtendedAgentCardRequest request,
        io.grpc.stub.StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetExtendedAgentCardMethod(), getCallOptions()), request, responseObserver);
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
   * A2AService defines the operations of the A2A protocol.
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
     * Send a message to the agent.
     * </pre>
     */
    public io.a2a.grpc.SendMessageResponse sendMessage(io.a2a.grpc.SendMessageRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming version of SendMessage.
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
    public io.a2a.grpc.Task getTask(io.a2a.grpc.GetTaskRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List tasks with optional filtering and pagination.
     * </pre>
     */
    public io.a2a.grpc.ListTasksResponse listTasks(io.a2a.grpc.ListTasksRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListTasksMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Cancel a task.
     * </pre>
     */
    public io.a2a.grpc.Task cancelTask(io.a2a.grpc.CancelTaskRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCancelTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SubscribeToTask allows subscribing to task updates for tasks not in terminal state.
     * Returns UnsupportedOperationError if task is in terminal state (completed, failed, canceled, rejected).
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, io.a2a.grpc.StreamResponse>
        subscribeToTask(io.a2a.grpc.SubscribeToTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getSubscribeToTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a push notification config for a task.
     * </pre>
     */
    public io.a2a.grpc.TaskPushNotificationConfig createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a push notification config for a task.
     * </pre>
     */
    public io.a2a.grpc.TaskPushNotificationConfig getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of push notifications configured for a task.
     * </pre>
     */
    public io.a2a.grpc.ListTaskPushNotificationConfigResponse listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetExtendedAgentCard returns the extended agent card for authenticated agents.
     * </pre>
     */
    public io.a2a.grpc.AgentCard getExtendedAgentCard(io.a2a.grpc.GetExtendedAgentCardRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetExtendedAgentCardMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a push notification config for a task.
     * </pre>
     */
    public com.google.protobuf.Empty deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteTaskPushNotificationConfigMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service A2AService.
   * <pre>
   * A2AService defines the operations of the A2A protocol.
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
     * Send a message to the agent.
     * </pre>
     */
    public io.a2a.grpc.SendMessageResponse sendMessage(io.a2a.grpc.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SendStreamingMessage is a streaming version of SendMessage.
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
     * List tasks with optional filtering and pagination.
     * </pre>
     */
    public io.a2a.grpc.ListTasksResponse listTasks(io.a2a.grpc.ListTasksRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListTasksMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Cancel a task.
     * </pre>
     */
    public io.a2a.grpc.Task cancelTask(io.a2a.grpc.CancelTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCancelTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SubscribeToTask allows subscribing to task updates for tasks not in terminal state.
     * Returns UnsupportedOperationError if task is in terminal state (completed, failed, canceled, rejected).
     * </pre>
     */
    public java.util.Iterator<io.a2a.grpc.StreamResponse> subscribeToTask(
        io.a2a.grpc.SubscribeToTaskRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSubscribeToTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a push notification config for a task.
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
     * GetExtendedAgentCard returns the extended agent card for authenticated agents.
     * </pre>
     */
    public io.a2a.grpc.AgentCard getExtendedAgentCard(io.a2a.grpc.GetExtendedAgentCardRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetExtendedAgentCardMethod(), getCallOptions(), request);
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
   * A2AService defines the operations of the A2A protocol.
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
     * Send a message to the agent.
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
     * List tasks with optional filtering and pagination.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.ListTasksResponse> listTasks(
        io.a2a.grpc.ListTasksRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListTasksMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Cancel a task.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.Task> cancelTask(
        io.a2a.grpc.CancelTaskRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a push notification config for a task.
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
     * GetExtendedAgentCard returns the extended agent card for authenticated agents.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<io.a2a.grpc.AgentCard> getExtendedAgentCard(
        io.a2a.grpc.GetExtendedAgentCardRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetExtendedAgentCardMethod(), getCallOptions()), request);
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
  private static final int METHODID_LIST_TASKS = 3;
  private static final int METHODID_CANCEL_TASK = 4;
  private static final int METHODID_SUBSCRIBE_TO_TASK = 5;
  private static final int METHODID_CREATE_TASK_PUSH_NOTIFICATION_CONFIG = 6;
  private static final int METHODID_GET_TASK_PUSH_NOTIFICATION_CONFIG = 7;
  private static final int METHODID_LIST_TASK_PUSH_NOTIFICATION_CONFIG = 8;
  private static final int METHODID_GET_EXTENDED_AGENT_CARD = 9;
  private static final int METHODID_DELETE_TASK_PUSH_NOTIFICATION_CONFIG = 10;

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
        case METHODID_LIST_TASKS:
          serviceImpl.listTasks((io.a2a.grpc.ListTasksRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.ListTasksResponse>) responseObserver);
          break;
        case METHODID_CANCEL_TASK:
          serviceImpl.cancelTask((io.a2a.grpc.CancelTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.a2a.grpc.Task>) responseObserver);
          break;
        case METHODID_SUBSCRIBE_TO_TASK:
          serviceImpl.subscribeToTask((io.a2a.grpc.SubscribeToTaskRequest) request,
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
        case METHODID_GET_EXTENDED_AGENT_CARD:
          serviceImpl.getExtendedAgentCard((io.a2a.grpc.GetExtendedAgentCardRequest) request,
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
          getListTasksMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.ListTasksRequest,
              io.a2a.grpc.ListTasksResponse>(
                service, METHODID_LIST_TASKS)))
        .addMethod(
          getCancelTaskMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.CancelTaskRequest,
              io.a2a.grpc.Task>(
                service, METHODID_CANCEL_TASK)))
        .addMethod(
          getSubscribeToTaskMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              io.a2a.grpc.SubscribeToTaskRequest,
              io.a2a.grpc.StreamResponse>(
                service, METHODID_SUBSCRIBE_TO_TASK)))
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
          getGetExtendedAgentCardMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.a2a.grpc.GetExtendedAgentCardRequest,
              io.a2a.grpc.AgentCard>(
                service, METHODID_GET_EXTENDED_AGENT_CARD)))
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
              .addMethod(getListTasksMethod())
              .addMethod(getCancelTaskMethod())
              .addMethod(getSubscribeToTaskMethod())
              .addMethod(getCreateTaskPushNotificationConfigMethod())
              .addMethod(getGetTaskPushNotificationConfigMethod())
              .addMethod(getListTaskPushNotificationConfigMethod())
              .addMethod(getGetExtendedAgentCardMethod())
              .addMethod(getDeleteTaskPushNotificationConfigMethod())
              .build();
        }
      }
    }
    return result;
  }
}
