package io.a2a.grpc.mapper;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    // 1. FAIL THE BUILD if fields are missing in either Spec or Proto
    unmappedTargetPolicy = ReportingPolicy.ERROR,

    // 2. Use the default component model (Singleton instance pattern)
    componentModel = "default",

    // 3. IGNORE null values when mapping to protobuf (builders don't accept null)
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface A2AProtoMapperConfig {

    // ========================================================================
    // 1. Enum Conversions
    // ========================================================================

    default String map(io.a2a.spec.APIKeySecurityScheme.Location location) {
        return location == null ? null : location.asString();
    }

    // ========================================================================
    // 2. Time & Duration
    // ========================================================================

    default Instant map(Timestamp value) {
        if (value == null) return null;
        return Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
    }

    default Timestamp map(Instant value) {
        if (value == null) return null;
        return Timestamp.newBuilder()
            .setSeconds(value.getEpochSecond())
            .setNanos(value.getNano())
            .build();
    }

    default java.time.Duration map(com.google.protobuf.Duration value) {
        if (value == null) return null;
        return java.time.Duration.ofSeconds(value.getSeconds(), value.getNanos());
    }

    default com.google.protobuf.Duration map(java.time.Duration value) {
        if (value == null) return null;
        return com.google.protobuf.Duration.newBuilder()
            .setSeconds(value.getSeconds())
            .setNanos(value.getNano())
            .build();
    }

    // ========================================================================
    // 3. Binary Data (ByteString)
    // ========================================================================

    default byte[] map(ByteString value) {
        return value == null ? null : value.toByteArray();
    }

    default ByteString map(byte[] value) {
        return value == null ? null : ByteString.copyFrom(value);
    }

    default ByteBuffer mapToBuffer(ByteString value) {
        return value == null ? null : value.asReadOnlyByteBuffer();
    }

    default ByteString mapFromBuffer(ByteBuffer value) {
        return value == null ? null : ByteString.copyFrom(value);
    }

    // ========================================================================
    // 4. Nullable Wrappers (Google Wrappers)
    // ========================================================================

    // String
    default String map(StringValue value) {
        return value == null ? null : value.getValue();
    }
    default StringValue mapString(String value) {
        return value == null ? null : StringValue.of(value);
    }

    // Integer
    default Integer map(Int32Value value) {
        return value == null ? null : value.getValue();
    }
    default Int32Value mapInt(Integer value) {
        return value == null ? null : Int32Value.of(value);
    }

    // Long
    default Long map(Int64Value value) {
        return value == null ? null : value.getValue();
    }
    default Int64Value mapLong(Long value) {
        return value == null ? null : Int64Value.of(value);
    }

    // Boolean
    default Boolean map(BoolValue value) {
        return value == null ? null : value.getValue();
    }
    default BoolValue mapBool(Boolean value) {
        return value == null ? null : BoolValue.of(value);
    }

    // ========================================================================
    // 5. JSON-RPC Support (Struct & Value)
    //    Maps "Struct" -> Map<String, Object>
    //    Maps "Value"  -> Object
    // ========================================================================

    default Map<String, Object> map(Struct struct) {
        if (struct == null) return null;
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
            map.put(entry.getKey(), map(entry.getValue()));
        }
        return map;
    }

    default Struct mapStruct(Map<String, Object> map) {
        if (map == null) return null;
        Struct.Builder builder = Struct.newBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.putFields(entry.getKey(), mapValue(entry.getValue()));
        }
        return builder.build();
    }

    default Object map(Value value) {
        if (value == null) return null;
        switch (value.getKindCase()) {
            case NULL_VALUE: return null;
            case NUMBER_VALUE: return value.getNumberValue(); // Returns Double
            case STRING_VALUE: return value.getStringValue();
            case BOOL_VALUE: return value.getBoolValue();
            case STRUCT_VALUE: return map(value.getStructValue());
            case LIST_VALUE:
                return value.getListValue().getValuesList().stream()
                        .map(this::map)
                        .collect(Collectors.toList());
            default: return null;
        }
    }

    default Value mapValue(Object object) {
        if (object == null) {
            return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
        }

        if (object instanceof String) return Value.newBuilder().setStringValue((String) object).build();
        if (object instanceof Boolean) return Value.newBuilder().setBoolValue((Boolean) object).build();
        if (object instanceof Number) return Value.newBuilder().setNumberValue(((Number) object).doubleValue()).build();
        if (object instanceof Map) {
            // Unchecked cast is unavoidable here unless we force Map<String, Object>
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            return Value.newBuilder().setStructValue(mapStruct(map)).build();
        }
        if (object instanceof List) {
            ListValue.Builder listBuilder = ListValue.newBuilder();
            for (Object item : (List<?>) object) {
                listBuilder.addValues(mapValue(item));
            }
            return Value.newBuilder().setListValue(listBuilder).build();
        }

        // Fallback for unknown types (e.g. custom objects inside Map) -> convert to String?
        // For now, throw to catch unexpected types early
        throw new IllegalArgumentException("Unsupported type for Proto Value conversion: " + object.getClass().getName());
    }
}