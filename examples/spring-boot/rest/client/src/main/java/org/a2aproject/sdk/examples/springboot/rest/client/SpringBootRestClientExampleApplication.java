package org.a2aproject.sdk.examples.springboot.rest.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SpringBootRestClientExampleProperties.class)
public class SpringBootRestClientExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootRestClientExampleApplication.class, args);
    }
}
