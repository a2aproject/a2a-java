package io.a2a.server.grpc.quarkus;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.server.apps.common.TestUtilsBean;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.transport.grpc.handler.GrpcHandler;

@Path("/test")
@ApplicationScoped
public class A2ATestResource {
    @Inject
    TestUtilsBean testUtilsBean;

    private final AtomicInteger streamingSubscribedCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        GrpcHandler.setStreamingSubscribedRunnable(streamingSubscribedCount::incrementAndGet);
    }


    @POST
    @Path("/task")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveTask(String body) throws Exception {
        Task task = JsonUtil.fromJson(body, Task.class);
        testUtilsBean.saveTask(task);
        return Response.ok().build();
    }

    @GET
    @Path("/task/{taskId}")
    public Response getTask(@PathParam("taskId") String taskId) throws Exception {
        Task task = testUtilsBean.getTask(taskId);
        if (task == null) {
            return Response.status(404).build();
        }
        return Response.ok()
                .entity(JsonUtil.toJson(task))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
    }

    @DELETE
    @Path("/task/{taskId}")
    public Response deleteTask(@PathParam("taskId") String taskId) {
        Task task = testUtilsBean.getTask(taskId);
        if (task == null) {
            return Response.status(404).build();
        }
        testUtilsBean.deleteTask(taskId);
        return Response.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path("/queue/ensure/{taskId}")
    public Response ensureQueue(@PathParam("taskId") String taskId) {
        testUtilsBean.ensureQueue(taskId);
        return Response.ok().build();
    }

    @POST
    @Path("/queue/enqueueTaskStatusUpdateEvent/{taskId}")
    public Response enqueueTaskStatusUpdateEvent(@PathParam("taskId") String taskId, String body) throws Exception {
        TaskStatusUpdateEvent event = JsonUtil.fromJson(body, TaskStatusUpdateEvent.class);
        testUtilsBean.enqueueEvent(taskId, event);
        return Response.ok().build();
    }

    @POST
    @Path("/queue/enqueueTaskArtifactUpdateEvent/{taskId}")
    public Response enqueueTaskArtifactUpdateEvent(@PathParam("taskId") String taskId, String body) throws Exception {
        TaskArtifactUpdateEvent event = JsonUtil.fromJson(body, TaskArtifactUpdateEvent.class);
        testUtilsBean.enqueueEvent(taskId, event);
        return Response.ok().build();
    }

    @GET
    @Path("/streamingSubscribedCount")
    @Produces(TEXT_PLAIN)
    public Response getStreamingSubscribedCount() {
        return Response.ok(String.valueOf(streamingSubscribedCount.get()), TEXT_PLAIN).build();
    }

    @GET
    @Path("/queue/childCount/{taskId}")
    @Produces(TEXT_PLAIN)
    public Response getChildQueueCount(@PathParam("taskId") String taskId) {
        int count = testUtilsBean.getChildQueueCount(taskId);
        return Response.ok(String.valueOf(count), TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/task/{taskId}/config/{configId}")
    public Response deleteTaskPushNotificationConfig(@PathParam("taskId") String taskId, @PathParam("configId") String configId) {
        Task task = testUtilsBean.getTask(taskId);
        if (task == null) {
            return Response.status(404).build();
        }
        testUtilsBean.deleteTaskPushNotificationConfig(taskId, configId);
        return Response.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path("/task/{taskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePushNotificationConfigInStore(@PathParam("taskId") String taskId, String body) throws Exception {
        TaskPushNotificationConfig notificationConfig = JsonUtil.fromJson(body, TaskPushNotificationConfig.class);
        if (notificationConfig == null) {
            return Response.status(404).build();
        }
        testUtilsBean.saveTaskPushNotificationConfig(taskId, notificationConfig);
        return Response.ok().build();
    }

    /**
     * REST endpoint to wait for queue poller to start.
     * Waits for the EventConsumer polling loop to start for the specified task's queue,
     * ensuring the queue is ready to receive and process events.
     *
     * @param taskId the task ID whose queue poller to wait for
     * @return HTTP 200 response when poller has started
     * @throws InterruptedException if interrupted while waiting
     */
    @POST
    @Path("/queue/awaitPollerStart/{taskId}")
    public Response awaitQueuePollerStart(@PathParam("taskId") String taskId) throws InterruptedException {
        testUtilsBean.awaitQueuePollerStart(taskId);
        return Response.ok().build();
    }

    /**
     * REST endpoint to wait for child queue count to stabilize.
     * Waits for the specified task's child queue count to match expectedCount for 3 consecutive
     * checks (150ms total), ensuring EventConsumer polling loops have started.
     *
     * @param taskId the task ID whose child queues to monitor
     * @param expectedCount the expected number of active child queues
     * @param timeoutMs maximum time to wait in milliseconds
     * @return HTTP 200 response with "true" if count stabilized, "false" if timeout occurred
     * @throws InterruptedException if interrupted while waiting
     */
    @POST
    @Path("/queue/awaitChildCountStable/{taskId}/{expectedCount}/{timeoutMs}")
    public Response awaitChildQueueCountStable(@PathParam("taskId") String taskId,
                                               @PathParam("expectedCount") int expectedCount,
                                               @PathParam("timeoutMs") long timeoutMs) throws InterruptedException {
        boolean stable = testUtilsBean.awaitChildQueueCountStable(taskId, expectedCount, timeoutMs);
        return Response.ok(String.valueOf(stable), TEXT_PLAIN).build();
    }
}
