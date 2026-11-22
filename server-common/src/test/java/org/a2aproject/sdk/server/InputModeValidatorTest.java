package org.a2aproject.sdk.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class InputModeValidatorTest {

    @Test
    void acceptsDeclaredPartMediaType() {
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("text/plain"),
                card(List.of("text/plain"), List.of())));
    }

    @Test
    void textInputModeAcceptsPlainTextButNotOtherTypes() {
        AgentCard card = card(List.of("text"), List.of());
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("Text/Plain; charset=utf-8"), card));
        assertThrows(ContentTypeNotSupportedError.class,
                () -> InputModeValidator.validate(messageWithMediaType("text/html"), card));
        assertThrows(ContentTypeNotSupportedError.class,
                () -> InputModeValidator.validate(messageWithMediaType("application/json"), card));
    }

    @Test
    void mediaInputModeAliasesAcceptTheirMediaTypeFamilies() {
        for (String mode : List.of("audio", "image", "video")) {
            AgentCard card = card(List.of(mode.toUpperCase(Locale.ROOT)), List.of());
            Message message = Message.builder().messageId("file").role(Message.Role.ROLE_USER)
                    .parts(new FilePart(new FileWithUri(mode + "/example", "file", "https://example.com/file")))
                    .build();
            assertDoesNotThrow(() -> InputModeValidator.validate(message, card));
            assertThrows(ContentTypeNotSupportedError.class,
                    () -> InputModeValidator.validate(messageWithMediaType("application/json"), card));
        }
    }

    @Test
    void acceptsMediaInputModeAliasDeclaredByASkill() {
        AgentSkill skill = AgentSkill.builder().id("audio").name("Audio").description("Handles audio")
                .tags(List.of()).inputModes(List.of("audio")).build();

        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("audio/wav"),
                card(List.of("text"), List.of(skill))));
    }

    @Test
    void matchesMediaTypeCaseParametersAndRanges() {
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("Text/Plain; charset=utf-8"),
                card(List.of("text/plain"), List.of())));
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("image/png"),
                card(List.of("IMAGE/*"), List.of())));
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("application/json"),
                card(List.of("*/*"), List.of())));
        assertThrows(ContentTypeNotSupportedError.class,
                () -> InputModeValidator.validate(messageWithMediaType("image/png"),
                        card(List.of("text/*"), List.of())));
        assertThrows(ContentTypeNotSupportedError.class,
                () -> InputModeValidator.validate(messageWithMediaType("text/plain; charset=iso-8859-1"),
                        card(List.of("text/plain; charset=utf-8"), List.of())));
    }

    @Test
    void comparesMimeParametersSemantically() {
        AgentCard card = card(List.of("text/plain; charset=utf-8; format=flowed"), List.of());
        assertDoesNotThrow(() -> InputModeValidator.validate(
                messageWithMediaType("Text/Plain; FORMAT=flowed; Charset=\"UTF-8\"; extra=value"), card));
        assertThrows(ContentTypeNotSupportedError.class, () -> InputModeValidator.validate(
                messageWithMediaType("text/plain; charset=utf-8; format=FLOWED"), card));
        assertThrows(ContentTypeNotSupportedError.class, () -> InputModeValidator.validate(
                messageWithMediaType("text/plain; charset=\"unterminated"), card));
        assertDoesNotThrow(() -> InputModeValidator.validate(
                messageWithMediaType("application/example; key=\"a;b\""),
                card(List.of("application/example; KEY=\"a;b\""), List.of())));
    }

    @Test
    void validatesMultiplePartsAgainstParsedInputModes() {
        AgentCard card = card(List.of("text/plain; charset=utf-8", "application/json", "invalid; key=\"broken"), List.of());
        Message message = Message.builder().messageId("multiple-parts").role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("first", null, "text/plain; Charset=\"UTF-8\""),
                        new TextPart("second", null, "application/json")))
                .build();
        assertDoesNotThrow(() -> InputModeValidator.validate(message, card));
        assertThrows(ContentTypeNotSupportedError.class,
                () -> InputModeValidator.validate(messageWithMediaType("invalid"), card));
    }

    @Test
    void rejectsUndeclaredPartMediaType() {
        ContentTypeNotSupportedError error = assertThrows(ContentTypeNotSupportedError.class,
                () -> InputModeValidator.validate(messageWithMediaType("image/png"),
                        card(List.of("text/plain"), List.of())));

        assertEquals("Input media type is not supported: image/png", error.getMessage());
    }

    @Test
    void acceptsMediaTypeDeclaredByASkill() {
        AgentSkill imageSkill = AgentSkill.builder()
                .id("image")
                .name("Image analysis")
                .description("Analyzes images")
                .tags(List.of())
                .inputModes(List.of("image/png"))
                .build();

        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("image/png"),
                card(List.of("text/plain"), List.of(imageSkill))));
    }

    @Test
    void acceptsExplicitAndOmittedMediaTypesWhenTheCardDeclaresNoInputModes() {
        AgentCard card = card(List.of(), List.of());

        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType("image/png"), card));
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType(null), card));
    }

    @Test
    void doesNotRejectPartWithoutAnExplicitMediaType() {
        assertDoesNotThrow(() -> InputModeValidator.validate(messageWithMediaType(null),
                card(List.of("text/plain"), List.of())));
    }

    private static Message messageWithMediaType(@Nullable String mediaType) {
        return Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(new TextPart("hello", null, mediaType))
                .messageId("message-1")
                .build();
    }

    private static AgentCard card(List<String> inputModes, List<AgentSkill> skills) {
        return AgentCard.builder()
                .name("Input validator test agent")
                .description("Input validator test agent")
                .supportedInterfaces(List.of(new AgentInterface("HTTP+JSON", "https://example.com/a2a")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(inputModes)
                .defaultOutputModes(List.of("text/plain"))
                .skills(skills)
                .build();
    }
}
