package org.a2aproject.sdk.integrations.springboot.server.tck.rest;

import static org.a2aproject.sdk.common.A2AHeaders.A2A_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootRestTckSutApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SpringBootRestTckSutSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesAgentCard() throws Exception {
        MvcResult result = mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("A2A Java SDK REST TCK SUT");
    }

    @Test
    void handlesTckMessageResponsePrefix() throws Exception {
        MessageSendParams params = MessageSendParams.builder()
                .message(Message.builder()
                        .role(Message.Role.ROLE_USER)
                        .messageId("tck-message-response-001")
                        .parts(new TextPart("hello"))
                        .build())
                .build();

        MvcResult result = mockMvc.perform(post("/message:send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(A2A_VERSION, AgentInterface.CURRENT_PROTOCOL_VERSION)
                        .content(JsonUtil.toJson(params)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Direct message response");
    }
}
