package io.a2a.grpc.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import io.a2a.spec.InvalidParamsError;

/**
 * Common field mapping utilities shared across all mappers.
 * <p>
 * Provides reusable conversion methods for common protobuf ↔ domain transformations:
 * <ul>
 * <li>Empty string → null conversion (protobuf optional string defaults)</li>
 * <li>Timestamp conversions (OffsetDateTime ↔ Protobuf Timestamp, Instant ↔ millis)</li>
 * <li>Metadata conversions (Map ↔ Protobuf Struct)</li>
 * <li>Empty list → null conversion (protobuf repeated field defaults)</li>
 * <li>Zero/false → null conversion (protobuf optional numeric/bool defaults)</li>
 * <li>Enum → null conversion (protobuf UNSPECIFIED/UNKNOWN handling)</li>
 * </ul>
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {TaskStateMapper.class})
public interface A2ACommonFieldMapper {

    A2ACommonFieldMapper INSTANCE = A2AMappers.getMapper(A2ACommonFieldMapper.class);

    /**
     * Converts protobuf empty strings to null for optional fields.
     * <p>
     * Protobuf optional strings return "" when unset, but domain models use null.
     * Use this with {@code @Mapping(qualifiedByName = "emptyToNull")}.
     *
     * @param value the protobuf string value
     * @return null if empty/null, otherwise the value
     */
    @Named("emptyToNull")
    default String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    /**
     * Validates that a required string field is not null or empty.
     * <p>
     * Throws an exception if the protobuf string is null or empty.
     * Use this with {@code @Mapping(qualifiedByName = "requireNonEmpty")}.
     *
     * @param value the protobuf string value
     * @return the value if not null/empty
     * @throws IllegalArgumentException if value is null or empty
     */
    @Named("requireNonEmpty")
    default String requireNonEmpty(String value) {
        if (value == null || value.isEmpty()) {
            throw new InvalidParamsError("Required field cannot be null or empty");
        }
        return value;
    }

    /**
     * Converts null strings to empty strings for protobuf.
     * <p>
     * Domain models use null for optional fields, but protobuf uses "".
     * Use this with {@code @Mapping(qualifiedByName = "nullToEmpty")}.
     *
     * @param value the domain string value
     * @return "" if null, otherwise the value
     */
    @Named("nullToEmpty")
    default String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Converts domain OffsetDateTime to protobuf Timestamp.
     * <p>
     * Use this with {@code @Mapping(qualifiedByName = "offsetDateTimeToProtoTimestamp")}.
     *
     * @param dateTime the domain OffsetDateTime
     * @return protobuf Timestamp, or default instance if input is null
     */
    @Named("offsetDateTimeToProtoTimestamp")
    default Timestamp offsetDateTimeToProtoTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = dateTime.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Converts protobuf Timestamp to domain OffsetDateTime (UTC).
     * <p>
     * Use this with {@code @Mapping(qualifiedByName = "protoTimestampToOffsetDateTime")}.
     *
     * @param timestamp the protobuf Timestamp
     * @return OffsetDateTime in UTC, or null if input is null/default
     */
    @Named("protoTimestampToOffsetDateTime")
    default OffsetDateTime protoTimestampToOffsetDateTime(Timestamp timestamp) {
        if (timestamp == null || timestamp.equals(Timestamp.getDefaultInstance())) {
            return null;
        }
        return OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneOffset.UTC
        );
    }

    /**
     * Converts empty lists to null for optional list fields.
     * <p>
     * Protobuf repeated fields return empty list when unset, but domain models may use null.
     * Use this with {@code @Mapping(qualifiedByName = "emptyListToNull")}.
     *
     * @param list the protobuf list
     * @return null if empty/null, otherwise the list
     */
    @Named("emptyListToNull")
    default <T> java.util.List<T> emptyListToNull(java.util.List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }

    /**
     * Converts domain Map to protobuf Struct (generic conversion).
     * <p>
     * Used for any {@code Map<String, Object>} field that maps to protobuf Struct (header, params, etc.).
     * Use this with {@code @Mapping(qualifiedByName = "mapToStruct")}.
     *
     * @param map the domain map
     * @return protobuf Struct, or default instance if input is null
     */
    @Named("mapToStruct")
    default Struct mapToStruct(Map<String, Object> map) {
        if (map == null) {
            return Struct.getDefaultInstance();
        }
        Struct.Builder structBuilder = Struct.newBuilder();
        map.forEach((k, v) -> structBuilder.putFields(k, objectToValue(v)));
        return structBuilder.build();
    }

    /**
     * Converts protobuf Struct to domain Map (generic conversion).
     * <p>
     * Used for any protobuf Struct field that maps to {@code Map<String, Object>} (header, params, etc.).
     * Use this with {@code @Mapping(qualifiedByName = "structToMap")}.
     *
     * @param struct the protobuf Struct
     * @return domain Map (may be null for empty Struct)
     */
    @Named("structToMap")
    default Map<String, Object> structToMap(Struct struct) {
        if (struct == null || struct.getFieldsCount() == 0) {
            return null;
        }
        return struct.getFieldsMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> valueToObject(e.getValue())));
    }

    /**
     * Converts a Java Object to protobuf Value.
     * <p>
     * Supports String, Number, Boolean, Map, List types, and null.
     * Used for struct conversion and arbitrary JSON data.
     *
     * @param value the Java object
     * @return protobuf Value
     */
    @SuppressWarnings("unchecked")
    default Value objectToValue(Object value) {
        Value.Builder valueBuilder = Value.newBuilder();
        if (value instanceof String) {
            valueBuilder.setStringValue((String) value);
        } else if (value instanceof Number) {
            valueBuilder.setNumberValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            valueBuilder.setBoolValue((Boolean) value);
        } else if (value instanceof Map) {
            valueBuilder.setStructValue(mapToStruct((Map<String, Object>) value));
        } else if (value instanceof List) {
            valueBuilder.setListValue(listToListValue((List<Object>) value));
        }
        return valueBuilder.build();
    }

    /**
     * Converts protobuf Value to Java Object.
     * <p>
     * Returns appropriate Java type based on Value's kind:
     * <ul>
     *   <li>STRUCT_VALUE -> {@code Map<String, Object>}</li>
     *   <li>LIST_VALUE -> {@code List<Object>}</li>
     *   <li>BOOL_VALUE -> {@code Boolean}</li>
     *   <li>NUMBER_VALUE -> {@code Double}</li>
     *   <li>STRING_VALUE -> {@code String}</li>
     *   <li>NULL_VALUE -> {@code null}</li>
     * </ul>
     * Used for struct conversion and arbitrary JSON data.
     *
     * @param value the protobuf Value
     * @return Java object (String, Double, Boolean, Map, List, or null)
     */
    default Object valueToObject(Value value) {
        switch (value.getKindCase()) {
            case STRUCT_VALUE:
                return structToMap(value.getStructValue());
            case LIST_VALUE:
                return value.getListValue().getValuesList().stream()
                        .map(this::valueToObject)
                        .collect(Collectors.toList());
            case BOOL_VALUE:
                return value.getBoolValue();
            case NUMBER_VALUE:
                return value.getNumberValue();
            case STRING_VALUE:
                return value.getStringValue();
            case NULL_VALUE:
            default:
                return null;
        }
    }

    /**
     * Converts Java List to protobuf ListValue.
     * <p>
     * Used for struct conversion and arbitrary JSON data.
     *
     * @param list the Java list
     * @return protobuf ListValue
     */
    default com.google.protobuf.ListValue listToListValue(List<Object> list) {
        com.google.protobuf.ListValue.Builder listValueBuilder = com.google.protobuf.ListValue.newBuilder();
        if (list != null) {
            list.forEach(o -> listValueBuilder.addValues(objectToValue(o)));
        }
        return listValueBuilder.build();
    }

    /**
     * Converts domain metadata Map to protobuf Struct.
     * <p>
     * Used for metadata fields in Artifact, Message, Task, and Events.
     * Use this with {@code @Mapping(qualifiedByName = "metadataToProto")}.
     *
     * @param metadata the domain metadata map
     * @return protobuf Struct, or default instance if input is null
     */
    @Named("metadataToProto")
    default Struct metadataToProto(Map<String, Object> metadata) {
        return mapToStruct(metadata);
    }

    /**
     * Converts protobuf Struct to domain metadata Map.
     * <p>
     * Used for metadata fields in Artifact, Message, Task, and Events.
     * Use this with {@code @Mapping(qualifiedByName = "metadataFromProto")}.
     *
     * @param struct the protobuf Struct
     * @return domain metadata Map (may be null for empty Struct)
     */
    @Named("metadataFromProto")
    default Map<String, Object> metadataFromProto(Struct struct) {
        if (struct == null || struct.getFieldsCount() == 0) {
            return Collections.emptyMap();
        }
        return structToMap(struct);
    }

    // ========================================================================
    // Optional Numeric/Boolean Conversions
    // ========================================================================
    /**
     * Converts protobuf int to Integer, treating 0 as null (unset).
     * <p>
     * Protobuf optional int32 fields default to 0 when unset, but domain models use null.
     * Use this with {@code @Mapping(qualifiedByName = "zeroToNull")}.
     *
     * @param value the protobuf int value
     * @return Integer or null if value is 0
     */
    @Named("zeroToNull")
    default Integer zeroToNull(int value) {
        return value != 0 ? value : null;
    }

    /**
     * Converts protobuf int to Integer, preserving all values including 0.
     * <p>
     * Unlike zeroToNull, this method preserves 0 values, allowing compact constructor
     * validation to catch invalid values (e.g., pageSize=0 must fail validation).
     * For truly optional fields where 0 means "unset", use zeroToNull instead.
     * Use this with {@code @Mapping(qualifiedByName = "intToIntegerOrNull")}.
     *
     * @param value the protobuf int value
     * @return Integer (never null for primitive int input)
     */
    @Named("intToIntegerOrNull")
    default Integer intToIntegerOrNull(int value) {
        return value;
    }

    /**
     * Converts protobuf long to Long, treating 0 as null (unset).
     * <p>
     * Protobuf optional int64 fields default to 0 when unset, but domain models use null.
     * Use this with {@code @Mapping(qualifiedByName = "zeroLongToNull")}.
     *
     * @param value the protobuf long value
     * @return Long or null if value is 0
     */
    @Named("zeroLongToNull")
    default Long zeroLongToNull(long value) {
        return value > 0L ? value : null;
    }

    /**
     * Converts protobuf bool to Boolean, treating false as null (unset).
     * <p>
     * Protobuf optional bool fields default to false when unset, but domain models use null.
     * Use this with {@code @Mapping(qualifiedByName = "falseToNull")}.
     *
     * @param value the protobuf bool value
     * @return Boolean or null if value is false
     */
    @Named("falseToNull")
    default Boolean falseToNull(boolean value) {
        return value ? true : null;
    }

    // ========================================================================
    // Instant ↔ Millis Conversions (for int64 timestamp fields)
    // ========================================================================
    /**
     * Converts domain Instant to protobuf milliseconds-since-epoch (int64).
     * <p>
     * Returns 0 if input is null (protobuf default for unset int64).
     * Use this with {@code @Mapping(qualifiedByName = "instantToMillis")}.
     *
     * @param instant the domain Instant
     * @return milliseconds since epoch, or 0 if null
     */
    @Named("instantToMillis")
    default long instantToMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : 0L;
    }

    /**
     * Converts protobuf milliseconds-since-epoch (int64) to domain Instant.
     * <p>
     * Returns null if input is 0 (protobuf default for unset field).
     * Throws InvalidParamsError for negative values (invalid timestamps).
     * Use this with {@code @Mapping(qualifiedByName = "millisToInstant")}.
     *
     * @param millis milliseconds since epoch
     * @return domain Instant, or null if millis is 0
     * @throws InvalidParamsError if millis is negative
     */
    @Named("millisToInstant")
    default Instant millisToInstant(long millis) {
        if (millis < 0L) {
            throw new InvalidParamsError(null,
                "Timestamp must be a non-negative number of milliseconds since epoch, but got: " + millis,
                null);
        }
        return millis > 0L ? Instant.ofEpochMilli(millis) : null;
    }

    // ========================================================================
    // Instant ↔ Timestamp Conversions (for Timestamp timestamp fields)
    // ========================================================================
    /**
     * Converts domain Instant to protobuf Timestamp.
     * <p>
     * Use this with {@code @Mapping(qualifiedByName = "instantToProtoTimestamp")}.
     *
     * @param instant the domain Instant
     * @return protobuf Timestamp, or default instance if input is null
     */
    @Named("instantToProtoTimestamp")
    default Timestamp instantToProtoTimestamp(Instant instant) {
        if (instant == null) {
            return Timestamp.getDefaultInstance();
        }
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Converts protobuf Timestamp to domain Instant.
     * <p>
     * Use this with {@code @Mapping(qualifiedByName = "protoTimestampToInstant")}.
     *
     * @param timestamp the protobuf Timestamp
     * @return Instant, or null if input is null/default
     */
    @Named("protoTimestampToInstant")
    default Instant protoTimestampToInstant(Timestamp timestamp) {
        if (timestamp == null || timestamp.equals(Timestamp.getDefaultInstance())) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    // ========================================================================
    // Enum Conversions (handling UNSPECIFIED/UNKNOWN)
    // ========================================================================
    /**
     * Converts protobuf TaskState to domain TaskState, treating UNSPECIFIED as null.
     * <p>
     * Protobuf enums default to UNSPECIFIED (0 value) when unset, which maps to null for optional fields.
     * However, UNRECOGNIZED (invalid enum values from JSON) throws InvalidParamsError for proper validation.
     * Use this with {@code @Mapping(qualifiedByName = "taskStateOrNull")}.
     *
     * @param state the protobuf TaskState
     * @return domain TaskState or null if UNSPECIFIED
     * @throws InvalidParamsError if state is UNRECOGNIZED (invalid enum value)
     */
    @Named("taskStateOrNull")
    default io.a2a.spec.TaskState taskStateOrNull(io.a2a.grpc.TaskState state) {
        if (state == null || state == io.a2a.grpc.TaskState.TASK_STATE_UNSPECIFIED) {
            return null;
        }
        // Reject invalid enum values (e.g., "INVALID_STATUS" from JSON)
        if (state == io.a2a.grpc.TaskState.UNRECOGNIZED) {
            String validStates = java.util.Arrays.stream(io.a2a.spec.TaskState.values())
                    .filter(s -> s != io.a2a.spec.TaskState.UNKNOWN)
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new InvalidParamsError(null,
                "Invalid task state value. Must be one of: " + validStates,
                null);
        }
        io.a2a.spec.TaskState result = TaskStateMapper.INSTANCE.fromProto(state);
        return result;
    }
}
