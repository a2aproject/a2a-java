package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.FileContent_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.spec.FileContent;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting FileContent types between A2A Protocol v0.3 and v1.0.
 * <p>
 * Handles polymorphic FileContent conversion for:
 * <ul>
 *   <li>{@link FileWithBytes_v0_3} ↔ {@link FileWithBytes}</li>
 *   <li>{@link FileWithUri_v0_3} ↔ {@link FileWithUri}</li>
 * </ul>
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: FileWithBytes and FileWithUri are simple records</li>
 *   <li>v1.0: FileWithBytes is a complex class with lazy loading; FileWithUri is a simple record</li>
 * </ul>
 * <p>
 * The conversion preserves the mimeType, name, and content (bytes or uri) fields across both versions.
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface FileContentMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    FileContentMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(FileContentMapper_v0_3.class);

    /**
     * Converts v0.3 FileContent to v1.0 FileContent.
     * <p>
     * Handles FileWithBytes and FileWithUri polymorphism using instanceof dispatch.
     *
     * @param v03 the v0.3 file content
     * @return the equivalent v1.0 file content
     * @throws InvalidRequestError if the file content type is unrecognized
     */
    default FileContent toV10(FileContent_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        if (v03 instanceof FileWithBytes_v0_3 v03Bytes) {
            return new FileWithBytes(v03Bytes.mimeType(), v03Bytes.name(), v03Bytes.bytes());
        } else if (v03 instanceof FileWithUri_v0_3 v03Uri) {
            return new FileWithUri(v03Uri.mimeType(), v03Uri.name(), v03Uri.uri());
        }

        throw new InvalidRequestError(null, "Unrecognized FileContent type: " + v03.getClass().getName(), null);
    }

    /**
     * Converts v1.0 FileContent to v0.3 FileContent.
     * <p>
     * Handles FileWithBytes and FileWithUri polymorphism using instanceof dispatch.
     *
     * @param v10 the v1.0 file content
     * @return the equivalent v0.3 file content
     * @throws InvalidRequestError if the file content type is unrecognized
     */
    default FileContent_v0_3 fromV10(FileContent v10) {
        if (v10 == null) {
            return null;
        }

        if (v10 instanceof FileWithBytes v10Bytes) {
            return new FileWithBytes_v0_3(
                v10Bytes.mimeType(),
                v10Bytes.name(),
                v10Bytes.bytes()
            );
        } else if (v10 instanceof FileWithUri v10Uri) {
            return new FileWithUri_v0_3(
                v10Uri.mimeType(),
                v10Uri.name(),
                v10Uri.uri()
            );
        }

        throw new InvalidRequestError(null, "Unrecognized FileContent type: " + v10.getClass().getName(), null);
    }
}
