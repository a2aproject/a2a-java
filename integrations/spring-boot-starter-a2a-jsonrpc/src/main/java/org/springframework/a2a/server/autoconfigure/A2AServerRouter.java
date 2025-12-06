package org.springframework.a2a.server.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
//@ConditionalOnMissingBean
public class A2AServerRouter {

    @Bean
    public RouterFunction<ServerResponse> route(A2AServerHandler a2aServerHandler) {
        return RouterFunctions
                .route(POST("/agent"), a2aServerHandler::invokeJSONRPCHandler)
                .andRoute(GET("/.well-known/agent-card.json"), a2aServerHandler::getAgentCard);
    }
}
