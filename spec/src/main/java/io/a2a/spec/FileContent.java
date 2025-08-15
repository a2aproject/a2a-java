package io.a2a.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Sealed interface representing file content in the A2A protocol.
 * <p>
 * This interface defines the contract for file content that can be transmitted
 * between agents. File content can be represented in two ways:
 * </p>
 * <ul>
 * <li>{@link FileWithBytes} - File content with embedded byte data</li>
 * <li>{@link FileWithUri} - File content referenced by a URI</li>
 * </ul>
 * <p>
 * The interface uses a custom deserializer to handle the polymorphic nature
 * of file content during JSON deserialization.
 * </p>
 */
@JsonDeserialize(using = FileContentDeserializer.class)
public sealed interface FileContent permits FileWithBytes, FileWithUri {

    /**
     * Gets the MIME type of the file content.
     *
     * @return the MIME type string (e.g., "text/plain", "image/jpeg")
     */
    String mimeType();

    /**
     * Gets the name of the file.
     *
     * @return the file name including extension
     */
    String name();
}
