package org.a2aproject.sdk.integrations.springboot.server.autoconfigure;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStateProvider;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Auto-configures the A2A runtime layer for Spring Boot applications.
 *
 * <p>This configuration owns the core server runtime beans only: config providers, task store,
 * event bus, queue manager, push-notification infrastructure, internal executors, and the
 * default {@link RequestHandler} wiring when an {@link AgentExecutor} is available.
 *
 * <p>The class deliberately does not depend on Servlet or Spring MVC APIs so the runtime layer
 * can activate in non-web Spring Boot applications as well.
 */
@AutoConfiguration
@EnableConfigurationProperties(A2ASpringBootProperties.class)
public class A2ARuntimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultValuesConfigProvider defaultValuesConfigProvider() {
        return new DefaultValuesConfigProvider();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(value = A2AConfigProvider.class, ignored = DefaultValuesConfigProvider.class)
    public A2AConfigProvider a2aConfigProvider(
            Environment environment,
            DefaultValuesConfigProvider defaultValuesConfigProvider
    ) {
        return new SpringEnvironmentA2AConfigProvider(environment, defaultValuesConfigProvider);
    }

    @Bean
    @ConditionalOnMissingBean(TaskStore.class)
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    @ConditionalOnMissingBean(MainEventBus.class)
    public MainEventBus mainEventBus() {
        return new MainEventBus();
    }

    @Bean
    @ConditionalOnMissingBean(QueueManager.class)
    public QueueManager queueManager(TaskStore taskStore, MainEventBus mainEventBus) {
        if (!(taskStore instanceof TaskStateProvider taskStateProvider)) {
            throw new IllegalStateException(
                    "Spring Boot A2A runtime requires a TaskStore that also implements TaskStateProvider.");
        }
        return new InMemoryQueueManager(taskStateProvider, mainEventBus);
    }

    @Bean
    @ConditionalOnMissingBean(MainEventBusProcessor.class)
    public MainEventBusProcessor mainEventBusProcessor(
            MainEventBus mainEventBus,
            TaskStore taskStore,
            PushNotificationSender pushNotificationSender,
            QueueManager queueManager
    ) {
        return new MainEventBusProcessor(mainEventBus, taskStore, pushNotificationSender, queueManager);
    }

    @Bean
    @ConditionalOnMissingBean(PushNotificationConfigStore.class)
    public PushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    @Bean
    @ConditionalOnMissingBean(PushNotificationSender.class)
    public PushNotificationSender pushNotificationSender(PushNotificationConfigStore pushNotificationConfigStore) {
        return new BasePushNotificationSender(pushNotificationConfigStore);
    }

    @Bean(name = "a2aInternalExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "a2aInternalExecutor")
    public ExecutorService a2aInternalExecutor(A2ASpringBootProperties properties) {
        A2ASpringBootProperties.Executor executor = properties.getExecutor();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(executor.getQueueCapacity()),
                new A2AInternalThreadFactory());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    @Bean(name = "a2aEventConsumerExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "a2aEventConsumerExecutor")
    public ExecutorService a2aEventConsumerExecutor() {
        return new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                10,
                TimeUnit.SECONDS,
                new java.util.concurrent.SynchronousQueue<>(),
                new A2AEventConsumerThreadFactory());
    }

    @Bean
    @ConditionalOnMissingBean(RequestHandler.class)
    public RequestHandler requestHandler(
            ObjectProvider<AgentExecutor> agentExecutorProvider,
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushNotificationConfigStore,
            MainEventBusProcessor mainEventBusProcessor,
            @Qualifier("a2aInternalExecutor") Executor internalExecutor,
            @Qualifier("a2aEventConsumerExecutor") Executor eventConsumerExecutor
    ) {
        AgentExecutor agentExecutor = agentExecutorProvider.getIfAvailable();
        if (agentExecutor == null) {
            return null;
        }
        return new DefaultRequestHandler(
                agentExecutor,
                taskStore,
                queueManager,
                pushNotificationConfigStore,
                mainEventBusProcessor,
                internalExecutor,
                eventConsumerExecutor
        );
    }
}
