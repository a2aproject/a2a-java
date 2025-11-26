package io.a2a.grpc.mapper;

import io.a2a.spec.DataPart;
import io.a2a.spec.FilePart;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.Part} and {@link io.a2a.grpc.Part}.
 * <p>
 * Handles polymorphic Part conversion by delegating to type-specific mappers:
 * <ul>
 *   <li>TextPart - handled directly (text field on proto Part)</li>
 *   <li>FilePart - delegated to FilePartMapper</li>
 *   <li>DataPart - delegated to DataPartMapper</li>
 * </ul>
 * <p>
 * <b>Manual Implementation Required:</b> Must use manual instanceof dispatch to handle protobuf oneof pattern
 * (text vs file vs data fields), as MapStruct's @SubclassMapping maps to different target types, not different fields of the same type.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {FilePartMapper.class, DataPartMapper.class})
public interface PartMapper {

    PartMapper INSTANCE = A2AMappers.getMapper(PartMapper.class);

    /**
     * Converts domain Part to proto Part.
     * Handles TextPart, FilePart, and DataPart polymorphism.
     */
    default io.a2a.grpc.Part toProto(Part<?> domain) {
        if (domain == null) {
            return null;
        }

        io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();

        if (domain instanceof TextPart textPart) {
            builder.setText(textPart.getText());
        } else if (domain instanceof FilePart filePart) {
            builder.setFile(FilePartMapper.INSTANCE.toProto(filePart));
        } else if (domain instanceof DataPart dataPart) {
            builder.setData(DataPartMapper.INSTANCE.toProto(dataPart));
        }

        return builder.build();
    }

    /**
     * Converts proto Part to domain Part.
     * Reconstructs TextPart, FilePart, or DataPart based on oneof field.
     */
    default Part<?> fromProto(io.a2a.grpc.Part proto) {
        if (proto == null) {
            return null;
        }

        if (proto.hasText()) {
            return new TextPart(proto.getText());
        } else if (proto.hasFile()) {
            return FilePartMapper.INSTANCE.fromProto(proto.getFile());
        } else if (proto.hasData()) {
            return DataPartMapper.INSTANCE.fromProto(proto.getData());
        }

        throw new InvalidRequestError();
    }
}
