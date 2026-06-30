package org.a2aproject.sdk.integrations.springboot.server.rest;

import org.a2aproject.sdk.integrations.springboot.server.autoconfigure.A2ARuntimeAutoConfiguration;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCard;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures the servlet-based A2A transport adapter.
 *
 * <p>This configuration is only active in servlet web applications and only contributes the
 * MVC-layer beans: the response mapper, exception handler, request mapper, and controller.
 * The core runtime beans are provided separately by {@link A2ARuntimeAutoConfiguration}.
 */
@AutoConfiguration(after = A2ARuntimeAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class A2AServletWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public A2ASpringBootHttpResponseMapper a2aSpringBootHttpResponseMapper() {
        return new A2ASpringBootHttpResponseMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2ASpringBootMvcExceptionHandler a2aSpringBootMvcExceptionHandler(A2ASpringBootHttpResponseMapper responseMapper) {
        return new A2ASpringBootMvcExceptionHandler(responseMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public A2APushNotificationConfigRequestMapper a2aPushNotificationConfigRequestMapper() {
        return new A2APushNotificationConfigRequestMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public A2ASpringBootMvcController a2aSpringBootMvcController(
            @Qualifier("agentCard") ObjectProvider<AgentCard> agentCardProvider,
            @Qualifier("extendedAgentCard") ObjectProvider<AgentCard> extendedAgentCard,
            ObjectProvider<RequestHandler> requestHandlerProvider,
            A2ASpringBootHttpResponseMapper responseMapper,
            A2APushNotificationConfigRequestMapper pushNotificationConfigRequestMapper,
            ObjectProvider<StreamingSubscriptionObserver> streamingSubscriptionObserver
    ) {
        AgentCard agentCard = agentCardProvider.getIfAvailable();
        RequestHandler requestHandler = requestHandlerProvider.getIfAvailable();
        if (agentCard == null || requestHandler == null) {
            return null;
        }
        return new A2ASpringBootMvcController(
                agentCard,
                extendedAgentCard,
                requestHandler,
                responseMapper,
                pushNotificationConfigRequestMapper,
                streamingSubscriptionObserver);
    }
}
