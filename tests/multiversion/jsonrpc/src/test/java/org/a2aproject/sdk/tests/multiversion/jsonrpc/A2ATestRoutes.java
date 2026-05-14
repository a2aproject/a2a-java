package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.apps.common.TestUtilsBean;
import org.a2aproject.sdk.server.apps.quarkus.A2AServerRoutes;
import org.a2aproject.sdk.compat03.server.apps.quarkus.A2AServerRoutes_v0_3;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;

@Singleton
public class A2ATestRoutes {
    @Inject
    TestUtilsBean testUtilsBean;

    @Inject
    A2AServerRoutes a2AServerRoutes;

    @Inject
    A2AServerRoutes_v0_3 a2AServerRoutes_v0_3;

    AtomicInteger streamingSubscribedCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        Runnable callback = () -> streamingSubscribedCount.incrementAndGet();
        A2AServerRoutes.setStreamingMultiSseSupportSubscribedRunnable(callback);
        A2AServerRoutes_v0_3.setStreamingMultiSseSupportSubscribedRunnable(callback);
    }

    void setupRoutes(@Observes Router router) {
        router.post("/test/task")
            .consumes(APPLICATION_JSON)
            .handler(BodyHandler.create())
            .blockingHandler(ctx -> {
                String body = ctx.body().asString();
                saveTask(body, ctx);
            });

        router.get("/test/task/:taskId")
            .produces(APPLICATION_JSON)
            .blockingHandler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                getTask(taskId, ctx);
            });

        router.delete("/test/task/:taskId")
            .blockingHandler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                deleteTask(taskId, ctx);
            });

        router.post("/test/queue/ensure/:taskId")
            .handler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                ensureTaskQueue(taskId, ctx);
            });

        router.post("/test/queue/enqueueTaskStatusUpdateEvent/:taskId")
            .handler(BodyHandler.create())
            .handler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                String body = ctx.body().asString();
                enqueueTaskStatusUpdateEvent(taskId, body, ctx);
            });

        router.post("/test/queue/enqueueTaskArtifactUpdateEvent/:taskId")
            .handler(BodyHandler.create())
            .handler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                String body = ctx.body().asString();
                enqueueTaskArtifactUpdateEvent(taskId, body, ctx);
            });

        router.get("/test/streamingSubscribedCount")
            .produces(TEXT_PLAIN)
            .handler(ctx -> getStreamingSubscribedCount(ctx));

        router.get("/test/queue/childCount/:taskId")
            .produces(TEXT_PLAIN)
            .handler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                getChildQueueCount(taskId, ctx);
            });

        router.delete("/test/task/:taskId/config/:configId")
            .blockingHandler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                String configId = ctx.pathParam("configId");
                deleteTaskPushNotificationConfig(taskId, configId, ctx);
            });

        router.post("/test/task/:taskId")
            .handler(BodyHandler.create())
            .blockingHandler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                String body = ctx.body().asString();
                saveTaskPushNotificationConfig(taskId, body, ctx);
            });

        router.post("/test/queue/awaitChildCountStable/:taskId/:expectedCount/:timeoutMs")
            .blockingHandler(ctx -> {
                String taskId = ctx.pathParam("taskId");
                String expectedCount = ctx.pathParam("expectedCount");
                String timeoutMs = ctx.pathParam("timeoutMs");
                awaitChildQueueCountStable(taskId, expectedCount, timeoutMs, ctx);
            });
    }

    public void saveTask(String body, RoutingContext rc) {
        try {
            Task task = JsonUtil.fromJson(body, Task.class);
            testUtilsBean.saveTask(task);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void getTask(String taskId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response().setStatusCode(404).end();
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

    public void deleteTask(String taskId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response().setStatusCode(404).end();
                return;
            }
            testUtilsBean.deleteTask(taskId);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void ensureTaskQueue(String taskId, RoutingContext rc) {
        try {
            testUtilsBean.ensureQueue(taskId);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void enqueueTaskStatusUpdateEvent(String taskId, String body, RoutingContext rc) {
        try {
            TaskStatusUpdateEvent event = JsonUtil.fromJson(body, TaskStatusUpdateEvent.class);
            testUtilsBean.enqueueEvent(taskId, event);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void enqueueTaskArtifactUpdateEvent(String taskId, String body, RoutingContext rc) {
        try {
            TaskArtifactUpdateEvent event = JsonUtil.fromJson(body, TaskArtifactUpdateEvent.class);
            testUtilsBean.enqueueEvent(taskId, event);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void getStreamingSubscribedCount(RoutingContext rc) {
        rc.response().setStatusCode(200).end(String.valueOf(streamingSubscribedCount.get()));
    }

    public void getChildQueueCount(String taskId, RoutingContext rc) {
        int count = testUtilsBean.getChildQueueCount(taskId);
        rc.response().setStatusCode(200).end(String.valueOf(count));
    }

    public void deleteTaskPushNotificationConfig(String taskId, String configId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response().setStatusCode(404).end();
                return;
            }
            testUtilsBean.deleteTaskPushNotificationConfig(taskId, configId);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void saveTaskPushNotificationConfig(String taskId, String body, RoutingContext rc) {
        try {
            TaskPushNotificationConfig notificationConfig = JsonUtil.fromJson(body, TaskPushNotificationConfig.class);
            if (notificationConfig == null) {
                rc.response().setStatusCode(404).end();
                return;
            }
            testUtilsBean.saveTaskPushNotificationConfig(taskId, notificationConfig);
            rc.response().setStatusCode(200).end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void awaitChildQueueCountStable(String taskId, String expectedCountStr, String timeoutMsStr, RoutingContext rc) {
        try {
            int expectedCount = Integer.parseInt(expectedCountStr);
            long timeoutMs = Long.parseLong(timeoutMsStr);
            boolean stable = testUtilsBean.awaitChildQueueCountStable(taskId, expectedCount, timeoutMs);
            rc.response().setStatusCode(200).end(String.valueOf(stable));
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    private void errorResponse(Throwable t, RoutingContext rc) {
        t.printStackTrace();
        rc.response().setStatusCode(500).putHeader(CONTENT_TYPE, TEXT_PLAIN).end();
    }
}
