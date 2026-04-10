package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = FileContentDeserializer.class)
public sealed interface FileContent permits FileWithBytes, FileWithUri {

    String mimeType();

    String name();
}
