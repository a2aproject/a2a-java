package org.a2aproject.sdk.compat03.client.adapter.jsonrpc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.jspecify.annotations.Nullable;

/** Retains original JSON values in fields that native protobuf interceptors have not changed. */
final class JSONRPCCompat03PayloadSupport {
    private JSONRPCCompat03PayloadSupport() {
    }

    static MessageSendParams_v0_3 preserveUnchangedValues(MessageSendParams_v0_3 original,
            MessageSendParams_v0_3 before, MessageSendParams_v0_3 after) {
        Message_v0_3 source = original.message();
        Message_v0_3 baseline = before.message();
        Message_v0_3 modified = after.message();
        List<Part_v0_3<?>> parts = modified.parts();
        if (baseline.parts().equals(modified.parts())) {
            parts = source.parts();
        } else if (source.parts().size() == 1 && baseline.parts().size() == 1 && modified.parts().size() == 1) {
            parts = List.of(restorePart(source.parts().get(0), baseline.parts().get(0), modified.parts().get(0)));
        } else if (hasLossyNumbers(source.parts(), baseline.parts())) {
            throw ambiguousArray();
        }
        Message_v0_3 message = new Message_v0_3(modified.role(), parts, modified.messageId(), modified.contextId(),
                modified.taskId(), modified.referenceTaskIds(),
                restoreMap(source.metadata(), baseline.metadata(), modified.metadata()), modified.extensions());
        return new MessageSendParams_v0_3(message, after.configuration(),
                restoreMap(original.metadata(), before.metadata(), after.metadata()));
    }

    static TaskIdParams_v0_3 preserveUnchangedValues(TaskIdParams_v0_3 original,
            TaskIdParams_v0_3 before, TaskIdParams_v0_3 after) {
        return new TaskIdParams_v0_3(after.id(), restoreMap(original.metadata(), before.metadata(), after.metadata()));
    }

    private static Part_v0_3<?> restorePart(Part_v0_3<?> original, Part_v0_3<?> before, Part_v0_3<?> after) {
        if (before.equals(after)) {
            return original;
        }
        @Nullable Map<String, Object> metadata = restoreMap(partMetadata(original), partMetadata(before), partMetadata(after));
        if (after instanceof DataPart_v0_3 modified) {
            Map<String, Object> data = modified.data();
            if (original instanceof DataPart_v0_3 source && before instanceof DataPart_v0_3 baseline) {
                data = Objects.requireNonNull(restoreMap(source.data(), baseline.data(), modified.data()));
            }
            return new DataPart_v0_3(data, metadata);
        }
        if (after instanceof TextPart_v0_3 modified) {
            return new TextPart_v0_3(modified.text(), metadata);
        }
        FilePart_v0_3 modified = (FilePart_v0_3) after;
        return new FilePart_v0_3(modified.file(), metadata);
    }

    private static @Nullable Map<String, Object> partMetadata(Part_v0_3<?> part) {
        if (part instanceof DataPart_v0_3 data) {
            return data.metadata();
        }
        if (part instanceof TextPart_v0_3 text) {
            return text.metadata();
        }
        return ((FilePart_v0_3) part).metadata();
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> restoreMap(@Nullable Map<String, Object> original,
            @Nullable Map<String, Object> before, @Nullable Map<String, Object> after) {
        return (Map<String, Object>) restore(original, before, after);
    }

    private static @Nullable Object restore(@Nullable Object original, @Nullable Object before, @Nullable Object after) {
        // Struct/Value uses doubles. Restore unchanged values directly from the original objects,
        // avoiding another JSON parse that could itself round large integers or decimal numbers.
        if (Objects.equals(before, after)) {
            return original;
        }
        if (original instanceof Map<?, ?> source && before instanceof Map<?, ?> baseline
                && after instanceof Map<?, ?> modified) {
            Map<Object, @Nullable Object> result = new LinkedHashMap<>();
            modified.forEach((key, value) -> result.put(key, source.containsKey(key) && baseline.containsKey(key)
                    ? restore(source.get(key), baseline.get(key), value) : value));
            return result;
        }
        if (original instanceof List<?> source && before instanceof List<?> baseline && after instanceof List<?> modified) {
            if (source.size() == 1 && baseline.size() == 1 && modified.size() == 1) {
                // Array slots have no identity. Only a singleton has an unambiguous slot.
                List<@Nullable Object> result = new ArrayList<>();
                result.add(restore(source.get(0), baseline.get(0), modified.get(0)));
                return result;
            }
            if (hasLossyNumbers(source, baseline)) {
                throw ambiguousArray();
            }
        }
        return after;
    }

    private static boolean hasLossyNumbers(@Nullable Object original, @Nullable Object projected) {
        if (original instanceof Number source && projected instanceof Number baseline) {
            return !Double.isFinite(baseline.doubleValue())
                    || new BigDecimal(source.toString()).compareTo(new BigDecimal(baseline.toString())) != 0;
        }
        if (original instanceof DataPart_v0_3 source && projected instanceof DataPart_v0_3 baseline) {
            return hasLossyNumbers(source.metadata(), baseline.metadata())
                    || hasLossyNumbers(source.data(), baseline.data());
        }
        if (original instanceof TextPart_v0_3 source && projected instanceof TextPart_v0_3 baseline) {
            return hasLossyNumbers(source.metadata(), baseline.metadata());
        }
        if (original instanceof FilePart_v0_3 source && projected instanceof FilePart_v0_3 baseline) {
            return hasLossyNumbers(source.metadata(), baseline.metadata());
        }
        if (original instanceof Map<?, ?> source && projected instanceof Map<?, ?> baseline) {
            for (var entry : source.entrySet()) {
                if (hasLossyNumbers(entry.getValue(), baseline.get(entry.getKey()))) {
                    return true;
                }
            }
        }
        if (original instanceof List<?> source && projected instanceof List<?> baseline) {
            for (int i = 0; i < source.size() && i < baseline.size(); i++) {
                if (hasLossyNumbers(source.get(i), baseline.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static A2AClientException ambiguousArray() {
        return new A2AClientException("Cannot preserve exact JSON numbers after a protobuf interceptor edits "
                + "a non-singleton array. Keep that array unchanged or use strings for exact numeric identifiers.");
    }
}
