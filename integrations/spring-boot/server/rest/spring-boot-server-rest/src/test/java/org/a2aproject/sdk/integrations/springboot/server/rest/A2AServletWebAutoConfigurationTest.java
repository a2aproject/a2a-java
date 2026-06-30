package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class A2AServletWebAutoConfigurationTest {

    private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2AServletWebAutoConfiguration.class))
            .withBean(AgentCard.class, this::agentCard)
            .withBean(RequestHandler.class, () -> Mockito.mock(RequestHandler.class));

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2AServletWebAutoConfiguration.class))
            .withBean(AgentCard.class, this::agentCard)
            .withBean(RequestHandler.class, () -> Mockito.mock(RequestHandler.class));

    @Test
    void doesNotCreateServletBeansInNonWebApplication() {
        nonWebContextRunner.run(context -> {
            assertFalse(context.containsBean("a2aSpringBootHttpResponseMapper"));
            assertFalse(context.containsBean("a2aSpringBootMvcController"));
        });
    }

    @Test
    void createsServletBeansWhenWebApplicationAndDependenciesExist() {
        webContextRunner.run(context -> {
            assertNotNull(context.getBean(A2ASpringBootHttpResponseMapper.class));
            assertNotNull(context.getBean(A2ASpringBootMvcController.class));
        });
    }

    @Test
    void respectsCustomMapperAndControllerBeans() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(A2AServletWebAutoConfiguration.class))
                .withUserConfiguration(CustomBeans.class)
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(A2ASpringBootHttpResponseMapper.class).size());
                    assertEquals(1, context.getBeansOfType(A2ASpringBootMvcController.class).size());
                    assertSame(context.getBean("customHttpResponseMapper"), context.getBean(A2ASpringBootHttpResponseMapper.class));
                    assertSame(context.getBean("customMvcController"), context.getBean(A2ASpringBootMvcController.class));
                });
    }

    private AgentCard agentCard() {
        return AgentCard.builder()
                .name("Spring Boot Test Agent")
                .description("Test agent for Spring Boot MVC transport")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().streaming(true).pushNotifications(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:8080")))
                .build();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomBeans {

        @Bean
        AgentCard agentCard() {
            return new A2AServletWebAutoConfigurationTest().agentCard();
        }

        @Bean
        RequestHandler requestHandler() {
            return Mockito.mock(RequestHandler.class);
        }

        @Bean
        A2ASpringBootHttpResponseMapper customHttpResponseMapper() {
            return new A2ASpringBootHttpResponseMapper();
        }

        @Bean
        A2ASpringBootMvcController customMvcController(
                AgentCard agentCard,
                RequestHandler requestHandler,
                A2ASpringBootHttpResponseMapper responseMapper
        ) {
            return new A2ASpringBootMvcController(
                    agentCard,
                    new StaticListableBeanFactory().getBeanProvider(AgentCard.class),
                    requestHandler,
                    responseMapper,
                    new A2APushNotificationConfigRequestMapper(),
                    new StaticListableBeanFactory().getBeanProvider(StreamingSubscriptionObserver.class));
        }
    }
}
