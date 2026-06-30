package org.a2aproject.sdk.integrations.springboot.server.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class A2ARuntimeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2ARuntimeAutoConfiguration.class))
            .withBean(AgentCard.class, this::agentCard)
            .withPropertyValues(
                    "a2a.executor.core-pool-size=1",
                    "a2a.executor.max-pool-size=1",
                    "a2a.executor.queue-capacity=1",
                    "a2a.executor.keep-alive-seconds=1");

    @Test
    void createsCoreRuntimeBeansWithoutWebEnvironment() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(DefaultValuesConfigProvider.class));
            assertNotNull(context.getBean(A2AConfigProvider.class));
            assertNotNull(context.getBean(TaskStore.class));
            assertNotNull(context.getBean(MainEventBus.class));
            assertNotNull(context.getBean(QueueManager.class));
            assertNotNull(context.getBean(MainEventBusProcessor.class));
            assertNotNull(context.getBean(PushNotificationConfigStore.class));
            assertNotNull(context.getBean(PushNotificationSender.class));
            assertTrue(context.getBeansOfType(RequestHandler.class).isEmpty());
        });
    }

    @Test
    void createsRequestHandlerOnlyWhenAgentExecutorIsPresent() {
        contextRunner.withBean(AgentExecutor.class, this::agentExecutor).run(context -> {
            assertInstanceOf(DefaultRequestHandler.class, context.getBean(RequestHandler.class));
        });
    }

    @Test
    void doesNotCreateRequestHandlerWithoutAgentExecutor() {
        contextRunner.run(context -> assertTrue(context.getBeansOfType(RequestHandler.class).isEmpty()));
    }

    @Test
    void preservesCustomTaskStoreAndExecutorBeans() {
        contextRunner
                .withBean(TaskStore.class, CustomTaskStore::new)
                .withBean("a2aInternalExecutor", ExecutorService.class, MarkerExecutorService::new)
                .withBean("a2aEventConsumerExecutor", ExecutorService.class, MarkerExecutorService::new)
                .withBean(AgentExecutor.class, this::agentExecutor)
                .run(context -> {
                    assertInstanceOf(CustomTaskStore.class, context.getBean(TaskStore.class));
                    assertInstanceOf(MarkerExecutorService.class, context.getBean("a2aInternalExecutor"));
                    assertInstanceOf(MarkerExecutorService.class, context.getBean("a2aEventConsumerExecutor"));
                    assertInstanceOf(DefaultRequestHandler.class, context.getBean(RequestHandler.class));
                });
    }

    @Test
    void springEnvironmentOverridesClasspathDefaults() {
        contextRunner.run(context -> {
            A2AConfigProvider provider = context.getBean(A2AConfigProvider.class);
            assertEquals("1", provider.getValue("a2a.executor.core-pool-size"));
        });
    }

    private AgentCard agentCard() {
        return AgentCard.builder()
                .name("Spring Boot Test Agent")
                .description("Test agent for Spring Boot integration")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().streaming(true).pushNotifications(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:8080")))
                .build();
    }

    private AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                emitter.sendMessage(new TextPart("ok").text());
                emitter.complete();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                emitter.cancel();
            }
        };
    }

    static final class CustomTaskStore extends InMemoryTaskStore {
    }

    static final class MarkerExecutorService extends AbstractExecutorService {

        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
