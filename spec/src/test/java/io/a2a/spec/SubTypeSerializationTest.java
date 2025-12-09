package io.a2a.spec;

import io.a2a.json.JsonProcessingException;
import io.a2a.json.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubTypeSerializationTest {

    private static final Task MINIMAL_TASK = new Task.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    @ParameterizedTest
    @MethodSource("serializationTestCases")
    void testSubtypeSerialization(Object objectToSerialize, String typePropertyName, String expectedTypeValue) throws JsonProcessingException {
        String json = JsonUtil.toJson(objectToSerialize);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = JsonUtil.fromJson(json, Map.class);
        assertEquals(expectedTypeValue, map.get(typePropertyName));
    }

    private static Stream<Arguments> serializationTestCases() {
        return Stream.of(
                Arguments.of(
                        new TaskStatusUpdateEvent.Builder()
                                .taskId(MINIMAL_TASK.getId())
                                .contextId(MINIMAL_TASK.getContextId())
                                .status(new TaskStatus(TaskState.COMPLETED))
                                .isFinal(true)
                                .build(), "kind", TaskStatusUpdateEvent.STATUS_UPDATE
                ),
                Arguments.of(
                        new TaskArtifactUpdateEvent.Builder()
                                .taskId(MINIMAL_TASK.getId())
                                .contextId(MINIMAL_TASK.getContextId())
                                .artifact(new Artifact.Builder()
                                        .artifactId("11")
                                        .parts(new TextPart("text"))
                                        .build())
                                .build(), "kind", TaskArtifactUpdateEvent.ARTIFACT_UPDATE
                ),
                Arguments.of(
                        MINIMAL_TASK, "kind", Task.TASK
                ),
                Arguments.of(
                        new Message.Builder()
                                .role(Message.Role.USER)
                                .parts(new TextPart("tell me some jokes"))
                                .contextId("context-1234")
                                .messageId("message-1234")
                                .build(), "kind", Message.MESSAGE
                ),
                Arguments.of(
                        new TextPart("text"), "kind", TextPart.TEXT
                ),
                Arguments.of(
                        new FilePart(new FileWithUri(
                                "image/jpeg", null, "file:///path/to/image.jpg")),
                        "kind", FilePart.FILE
                ),
                Arguments.of(
                        new DataPart(Map.of("chartType", "bar")), "kind", DataPart.DATA
                ),
                Arguments.of(
                        new APIKeySecurityScheme.Builder()
                                .in("test").name("name").description("description").build(),
                        "type", APIKeySecurityScheme.API_KEY
                ),
                Arguments.of(
                        new HTTPAuthSecurityScheme.Builder()
                        .scheme("basic").description("Basic Auth").build(),
                        "type", HTTPAuthSecurityScheme.HTTP
                ),
                Arguments.of(
                        new OAuth2SecurityScheme.Builder()
                                .flows(new OAuthFlows.Builder().build())
                                .description("oAuth2SecurityScheme").build(),
                        "type", OAuth2SecurityScheme.OAUTH2
                ),
                Arguments.of(
                        new OpenIdConnectSecurityScheme.Builder()
                                .openIdConnectUrl("https://accounts.google.com/.well-known/openid-configuration")
                                .description("OpenId").build(),
                        "type", OpenIdConnectSecurityScheme.OPENID_CONNECT
                ),
                Arguments.of(
                        new MutualTLSSecurityScheme("mutual tls test"),
                        "type", MutualTLSSecurityScheme.MUTUAL_TLS
                )
        );
    }

}
