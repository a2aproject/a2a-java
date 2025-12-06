package com.example.demo_adk;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
public class A2AConfiguration {


    @Bean
    public RequestHandler requestHandler(
            AgentExecutor agentExecutor,
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushNotificationConfigStore,
            Executor executor) {
        // Push notifications not used, pass null
        return DefaultRequestHandler.create(
                agentExecutor,
                taskStore,
                queueManager,
                pushNotificationConfigStore,
                null,  // pushNotificationSender
                executor);
    }

    @Bean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    public QueueManager queueManager(TaskStore taskStore) {
        // InMemoryTaskStore implements both TaskStore and TaskStateProvider
        return new InMemoryQueueManager((TaskStateProvider) taskStore);
    }

    @Bean
    public PushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    @Bean
    public A2AConfigProvider a2aConfigProvider() {
        return new DefaultValuesConfigProvider();
    }





}
