package org.springframework.a2a.server.autoconfigure;

import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import org.springframework.a2a.server.autoconfigure.properties.JsonRpcProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnClass(JSONRPCHandler.class)
@EnableConfigurationProperties(JsonRpcProperties.class)
@ConditionalOnProperty(prefix = "spring.a2a.jsonrpc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JsonRpcAutoConfiguration {

    /**
     * Provides a default executor for internal async operations.
     * Users can override this by defining their own bean named "internalExecutor".
     */
    @Bean
    @ConditionalOnMissingBean
    public Executor internalExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Creates the JSONRPCHandler bean from the a2a-java-sdk.
     * This requires the user of the starter to provide an AgentCard bean and a RequestHandler bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public JSONRPCHandler jsonRpcSdkHandler(
            AgentCard agentCard, // User must provide a bean of type AgentCard. If multiple, one must be @Primary.
            RequestHandler requestHandler, // User must provide a bean of type RequestHandler.
            Executor executor) {
        // We use the simpler constructor. The @PublicAgentCard qualifier from CDI is not needed
        // as long as the user provides a single primary AgentCard bean.
        return new JSONRPCHandler(agentCard, requestHandler, executor);
    }

}