package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubTypeSerialization_v0_3_Test {

    private static final Task_v0_3 MINIMAL_TASK = new Task_v0_3.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
            .build();

    @ParameterizedTest
    @MethodSource("serializationTestCases")
    void testSubtypeSerialization(Object objectToSerialize, String typePropertyName, String expectedTypeValue) throws JsonProcessingException_v0_3 {
        String json = JsonUtil_v0_3.toJson(objectToSerialize);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = JsonUtil_v0_3.fromJson(json, Map.class);
        assertEquals(expectedTypeValue, map.get(typePropertyName));
    }

    private static Stream<Arguments> serializationTestCases() {
        return Stream.of(
                Arguments.of(
                        new TaskStatusUpdateEvent_v0_3.Builder()
                                .taskId(MINIMAL_TASK.getId())
                                .contextId(MINIMAL_TASK.getContextId())
                                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                                .isFinal(true)
                                .build(), "kind", TaskStatusUpdateEvent_v0_3.STATUS_UPDATE
                ),
                Arguments.of(
                        new TaskArtifactUpdateEvent_v0_3.Builder()
                                .taskId(MINIMAL_TASK.getId())
                                .contextId(MINIMAL_TASK.getContextId())
                                .artifact(new Artifact_v0_3.Builder()
                                        .artifactId("11")
                                        .parts(new TextPart_v0_3("text"))
                                        .build())
                                .build(), "kind", TaskArtifactUpdateEvent_v0_3.ARTIFACT_UPDATE
                ),
                Arguments.of(
                        MINIMAL_TASK, "kind", Task_v0_3.TASK
                ),
                Arguments.of(
                        new Message_v0_3.Builder()
                                .role(Message_v0_3.Role.USER)
                                .parts(new TextPart_v0_3("tell me some jokes"))
                                .contextId("context-1234")
                                .messageId("message-1234")
                                .build(), "kind", Message_v0_3.MESSAGE
                ),
                Arguments.of(
                        new TextPart_v0_3("text"), "kind", TextPart_v0_3.TEXT
                ),
                Arguments.of(
                        new FilePart_v0_3(new FileWithUri_v0_3(
                                "image/jpeg", null, "file:///path/to/image.jpg")),
                        "kind", FilePart_v0_3.FILE
                ),
                Arguments.of(
                        new DataPart_v0_3(Map.of("chartType", "bar")), "kind", DataPart_v0_3.DATA
                ),
                Arguments.of(
                        new APIKeySecurityScheme_v0_3.Builder()
                                .in("test").name("name").description("description").build(),
                        "type", APIKeySecurityScheme_v0_3.API_KEY
                ),
                Arguments.of(
                        new HTTPAuthSecurityScheme_v0_3.Builder()
                        .scheme("basic").description("Basic Auth").build(),
                        "type", HTTPAuthSecurityScheme_v0_3.HTTP
                ),
                Arguments.of(
                        new OAuth2SecurityScheme_v0_3.Builder()
                                .flows(new OAuthFlows_v0_3.Builder().build())
                                .description("oAuth2SecurityScheme").build(),
                        "type", OAuth2SecurityScheme_v0_3.OAUTH2
                ),
                Arguments.of(
                        new OpenIdConnectSecurityScheme_v0_3.Builder()
                                .openIdConnectUrl("https://accounts.google.com/.well-known/openid-configuration")
                                .description("OpenId").build(),
                        "type", OpenIdConnectSecurityScheme_v0_3.OPENID_CONNECT
                ),
                Arguments.of(
                        new MutualTLSSecurityScheme_v0_3("mutual tls test"),
                        "type", MutualTLSSecurityScheme_v0_3.MUTUAL_TLS
                )
        );
    }

}
