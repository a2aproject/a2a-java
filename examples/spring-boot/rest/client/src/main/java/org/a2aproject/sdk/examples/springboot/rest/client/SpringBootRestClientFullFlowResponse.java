package org.a2aproject.sdk.examples.springboot.rest.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.a2aproject.sdk.spec.AgentCard;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpringBootRestClientFullFlowResponse(
        boolean success,
        String serverUrl,
        AgentCard agentCard,
        SpringBootRestClientScenarioResponse blocking,
        SpringBootRestClientScenarioResponse streaming,
        String errorMessage) {

    public static SpringBootRestClientFullFlowResponse success(
            String serverUrl,
            AgentCard agentCard,
            SpringBootRestClientScenarioResponse blocking,
            SpringBootRestClientScenarioResponse streaming) {
        return new SpringBootRestClientFullFlowResponse(true, serverUrl, agentCard, blocking, streaming, null);
    }

    public static SpringBootRestClientFullFlowResponse failure(String serverUrl, String errorMessage) {
        return new SpringBootRestClientFullFlowResponse(false, serverUrl, null, null, null, errorMessage);
    }
}
