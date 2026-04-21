package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting Part types between A2A Protocol v0.3 and v1.0.
 * <p>
 * Handles polymorphic Part conversion for:
 * <ul>
 *   <li>{@link TextPart_v0_3} ↔ {@link TextPart}</li>
 *   <li>{@link FilePart_v0_3} ↔ {@link FilePart}</li>
 *   <li>{@link DataPart_v0_3} ↔ {@link DataPart}</li>
 * </ul>
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: Part types are classes with getter methods (e.g., {@code getText()}, {@code getMetadata()})</li>
 *   <li>v1.0: Part types are records with accessor methods (e.g., {@code text()}, {@code metadata()})</li>
 * </ul>
 * <p>
 * Uses manual instanceof dispatch to handle polymorphic conversion.
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface PartMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    PartMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(PartMapper_v0_3.class);

    /**
     * Converts v0.3 Part to v1.0 Part.
     * <p>
     * Handles TextPart, FilePart, and DataPart polymorphism using instanceof dispatch.
     *
     * @param v03 the v0.3 part
     * @return the equivalent v1.0 part
     * @throws InvalidRequestError if the part type is unrecognized
     */
    default Part<?> toV10(Part_v0_3<?> v03) {
        if (v03 == null) {
            return null;
        }

        if (v03 instanceof TextPart_v0_3 v03Text) {
            return new TextPart(v03Text.getText(), v03Text.getMetadata());
        } else if (v03 instanceof FilePart_v0_3 v03File) {
            return new FilePart(
                FileContentMapper_v0_3.INSTANCE.toV10(v03File.getFile()),
                v03File.getMetadata()
            );
        } else if (v03 instanceof DataPart_v0_3 v03Data) {
            return new DataPart(v03Data.getData(), v03Data.getMetadata());
        }

        throw new InvalidRequestError(null, "Unrecognized Part type: " + v03.getClass().getName(), null);
    }

    /**
     * Converts v1.0 Part to v0.3 Part.
     * <p>
     * Handles TextPart, FilePart, and DataPart polymorphism using instanceof dispatch.
     *
     * @param v10 the v1.0 part
     * @return the equivalent v0.3 part
     * @throws InvalidRequestError if the part type is unrecognized
     */
    default Part_v0_3<?> fromV10(Part<?> v10) {
        if (v10 == null) {
            return null;
        }

        if (v10 instanceof TextPart v10Text) {
            return new TextPart_v0_3(v10Text.text(), v10Text.metadata());
        } else if (v10 instanceof FilePart v10File) {
            return new FilePart_v0_3(
                FileContentMapper_v0_3.INSTANCE.fromV10(v10File.file()),
                v10File.metadata()
            );
        } else if (v10 instanceof DataPart v10Data) {
            // v1.0 DataPart.data() returns Object, but v0.3 expects Map<String, Object>
            Object data = v10Data.data();
            if (!(data instanceof java.util.Map)) {
                throw new InvalidRequestError(null, "DataPart data must be a Map for v0.3 compatibility", null);
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
            return new DataPart_v0_3(dataMap, v10Data.metadata());
        }

        throw new InvalidRequestError(null, "Unrecognized Part type: " + v10.getClass().getName(), null);
    }
}
