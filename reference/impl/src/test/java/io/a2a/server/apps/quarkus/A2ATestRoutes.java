package io.a2a.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.a2a.server.apps.common.TestUtilsBean;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
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
            Task task = Utils.OBJECT_MAPPER.readValue(body, Task.class);
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
                    .end(Utils.OBJECT_MAPPER.writeValueAsString(task));

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
            TaskStatusUpdateEvent event = Utils.OBJECT_MAPPER.readValue(body, TaskStatusUpdateEvent.class);
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
            TaskArtifactUpdateEvent event = Utils.OBJECT_MAPPER.readValue(body, TaskArtifactUpdateEvent.class);
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
            PushNotificationConfig notificationConfig = Utils.OBJECT_MAPPER.readValue(body, PushNotificationConfig.class);
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

    private void errorResponse(Throwable t, RoutingContext rc) {
        t.printStackTrace();
        rc.response()
                .setStatusCode(500)
                .putHeader(CONTENT_TYPE, TEXT_PLAIN)
                .end();
    }

}
