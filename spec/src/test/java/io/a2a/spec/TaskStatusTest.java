package io.a2a.spec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TaskStatusTest {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final String REPLACE_TIMESTAMP_PATTERN = ".*\"timestamp\":\"([^\"]+)\",?.*";

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
    }

    @Test
    public void testTaskStatusWithSetTimestamp() {
        TaskState state = TaskState.WORKING;
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2023-10-01T12:00:00Z");
        TaskStatus status = new TaskStatus(state, offsetDateTime);

        assertNotNull(status.timestamp());
        assertEquals(offsetDateTime, status.timestamp());
    }

    @Test
    public void testTaskStatusWithProvidedTimestamp() {
        OffsetDateTime providedTimestamp = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        TaskState state = TaskState.COMPLETED;
        TaskStatus status = new TaskStatus(state, providedTimestamp);

        assertEquals(providedTimestamp, status.timestamp());
    }

    @Test
    public void testTaskStatusSerializationUsesISO8601Format() throws Exception {
        OffsetDateTime expectedTimestamp = OffsetDateTime.parse("2023-10-01T12:00:00.234-05:00");
        TaskState state = TaskState.WORKING;
        TaskStatus status = new TaskStatus(state, expectedTimestamp);

        String json = OBJECT_MAPPER.writeValueAsString(status);

        String expectedJson = "{\"state\":\"working\",\"timestamp\":\"2023-10-01T12:00:00.234-05:00\"}";
        assertEquals(expectedJson, json);
    }

    @Test
    public void testTaskStatusDeserializationWithValidISO8601Format() throws Exception {
        String validJson = "{"
                + "\"state\": \"auth-required\","
                + "\"timestamp\": \"2023-10-01T12:00:00.10+03:00\""
                + "}";

        TaskStatus result = OBJECT_MAPPER.readValue(validJson, TaskStatus.class);
        assertEquals(TaskState.AUTH_REQUIRED, result.state());
        assertNotNull(result.timestamp());
        assertEquals(OffsetDateTime.parse("2023-10-01T12:00:00.100+03:00"), result.timestamp());
    }

    @Test
    public void testTaskStatusDeserializationWithInvalidISO8601FormatFails() {
        String invalidJson = "{"
                + "\"state\": \"completed\","
                + "\"timestamp\": \"2023/10/01 12:00:00\""
                + "}";

        assertThrows(
                com.fasterxml.jackson.databind.exc.InvalidFormatException.class,
                () -> OBJECT_MAPPER.readValue(invalidJson, TaskStatus.class)
        );
    }

    @Test
    public void testTaskStatusJsonTimestampMatchesISO8601Regex() throws Exception {
        TaskState state = TaskState.WORKING;
        OffsetDateTime expectedTimestamp = OffsetDateTime.parse("2023-10-01T12:00:00.234Z");
        TaskStatus status = new TaskStatus(state, expectedTimestamp);

        String json = OBJECT_MAPPER.writeValueAsString(status);

        String timestampValue = json.replaceAll(REPLACE_TIMESTAMP_PATTERN, "$1");
        assertEquals(expectedTimestamp, OffsetDateTime.parse(timestampValue));
    }
}
