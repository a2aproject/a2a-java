package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.spec.Message.MESSAGE;
import static org.a2aproject.sdk.compat03.spec.Task.TASK;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "kind",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Task.class, name = TASK),
        @JsonSubTypes.Type(value = Message.class, name = MESSAGE)
})
public interface EventKind {

    String getKind();
}
