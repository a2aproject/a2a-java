package org.springframework.a2a.server.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .anyExchange()
                        .permitAll())
//                .authorizeExchange(exchange -> exchange
//                        .pathMatchers("/.well-known/agent-card.json")
//                        .permitAll()
//                        .anyExchange()
//                        .authenticated())
//                .permitAll())
                .build();
    }
}
