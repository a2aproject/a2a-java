package org.a2aproject.sdk.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.apps.common.TestUtilsBean;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Exposes the {@link TestUtilsBean} via REST using Quarkus Reactive Routes
 */
@Singleton
public class A2ATestRoutes {
    @Inject
    TestUtilsBean testUtilsBean;

    @Inject
    A2AServerRoutes a2AServerRoutes;

    AtomicInteger streamingSubscribedCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        A2AServerRoutes.setStreamingMultiSseSupportSubscribedRunnable(() -> streamingSubscribedCount.incrementAndGet());
    }


    @Route(path = "/test/task", methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void saveTask(@Body String body, RoutingContext rc) {
        try {
            Task task = JsonUtil.fromJson(body, Task.class);
            testUtilsBean.saveTask(task);
            rc.response()
                .setStatusCode(200)
                .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.GET}, produces = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void getTask(@Param String taskId,  RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                    .setStatusCode(404)
                    .end();
                return;
            }
            rc.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end(JsonUtil.toJson(task));

        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTask(@Param String taskId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                        .setStatusCode(404)
                        .end();
                return;
            }
            testUtilsBean.deleteTask(taskId);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/ensure/:taskId", methods = {Route.HttpMethod.POST})
    public void ensureTaskQueue(@Param String taskId, RoutingContext rc) {
        try {
            testUtilsBean.ensureQueue(taskId);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/enqueueTaskStatusUpdateEvent/:taskId", methods = {Route.HttpMethod.POST})
    public void enqueueTaskStatusUpdateEvent(@Param String taskId, @Body String body, RoutingContext rc) {

        try {
            TaskStatusUpdateEvent event = JsonUtil.fromJson(body, TaskStatusUpdateEvent.class);
            testUtilsBean.enqueueEvent(taskId, event);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/enqueueTaskArtifactUpdateEvent/:taskId", methods = {Route.HttpMethod.POST})
    public void enqueueTaskArtifactUpdateEvent(@Param String taskId, @Body String body, RoutingContext rc) {

        try {
            TaskArtifactUpdateEvent event = JsonUtil.fromJson(body, TaskArtifactUpdateEvent.class);
            testUtilsBean.enqueueEvent(taskId, event);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/streamingSubscribedCount", methods = {Route.HttpMethod.GET}, produces = {TEXT_PLAIN})
    public void getStreamingSubscribedCount(RoutingContext rc) {
        rc.response()
                .setStatusCode(200)
                .end(String.valueOf(streamingSubscribedCount.get()));
    }

    @Route(path = "/test/queue/childCount/:taskId", methods = {Route.HttpMethod.GET}, produces = {TEXT_PLAIN})
    public void getChildQueueCount(@Param String taskId, RoutingContext rc) {
        int count = testUtilsBean.getChildQueueCount(taskId);
        rc.response()
                .setStatusCode(200)
                .end(String.valueOf(count));
    }

    @Route(path = "/test/task/:taskId/config/:configId", methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTaskPushNotificationConfig(@Param String taskId, @Param String configId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                        .setStatusCode(404)
                        .end();
                return;
            }
            testUtilsBean.deleteTaskPushNotificationConfig(taskId, configId);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void saveTaskPushNotificationConfig(@Param String taskId, @Body String body, RoutingContext rc) {
        try {
            TaskPushNotificationConfig notificationConfig = JsonUtil.fromJson(body, TaskPushNotificationConfig.class);
            if (notificationConfig == null) {
                rc.response()
                        .setStatusCode(404)
                        .end();
                return;
            }
            testUtilsBean.saveTaskPushNotificationConfig(taskId, notificationConfig);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    /**
     * REST endpoint to wait for child queue count to stabilize.
     * Waits for the specified task's child queue count to match expectedCount for 3 consecutive
     * checks (150ms total), ensuring EventConsumer polling loops have started.
     *
     * @param taskId the task ID whose child queues to monitor
     * @param expectedCountStr the expected number of active child queues (as string)
     * @param timeoutMsStr maximum time to wait in milliseconds (as string)
     * @param rc the Vert.x routing context
     */
    @Route(path = "/test/queue/awaitChildCountStable/:taskId/:expectedCount/:timeoutMs", methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void awaitChildQueueCountStable(@Param("taskId") String taskId, @Param("expectedCount") String expectedCountStr, @Param("timeoutMs") String timeoutMsStr, RoutingContext rc) {
        try {
            int expectedCount = Integer.parseInt(expectedCountStr);
            long timeoutMs = Long.parseLong(timeoutMsStr);
            boolean stable = testUtilsBean.awaitChildQueueCountStable(taskId, expectedCount, timeoutMs);
            rc.response()
                    .setStatusCode(200)
                    .end(String.valueOf(stable));
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    private void errorResponse(Throwable t, RoutingContext rc) {
        t.printStackTrace();
        rc.response()
                .setStatusCode(500)
                .putHeader(CONTENT_TYPE, TEXT_PLAIN)
                .end();
    }

}
