package org.a2aproject.sdk.grpc.mapper;

import java.util.Base64;

import com.google.protobuf.ByteString;
import com.google.protobuf.Value;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FileContent;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import java.util.Map;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.Part} and {@link org.a2aproject.sdk.grpc.Part}.
 * <p>
 * Handles polymorphic Part conversion using the proto's oneof content field:
 * <ul>
 *   <li>TextPart - maps to Part.text</li>
 *   <li>FilePart(FileWithBytes) - maps to Part.raw + Part.filename + Part.media_type</li>
 *   <li>FilePart(FileWithUri) - maps to Part.url + Part.filename + Part.media_type</li>
 *   <li>DataPart - maps to Part.data (google.protobuf.Value containing any JSON value: object, array, primitive, or null)</li>
 * </ul>
 * <p>
 * <b>Manual Implementation Required:</b> Must use manual instanceof dispatch to handle protobuf oneof pattern,
 * as MapStruct's @SubclassMapping maps to different target types, not different fields of the same type.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {A2ACommonFieldMapper.class})
public interface PartMapper {

    PartMapper INSTANCE = A2AMappers.getMapper(PartMapper.class);

    /**
     * Converts domain Part to proto Part.
     * Handles TextPart, FilePart (FileWithBytes and FileWithUri), and DataPart polymorphism.
     */
    default org.a2aproject.sdk.grpc.Part toProto(Part<?> domain) {
        if (domain == null) {
            return null;
        }

        org.a2aproject.sdk.grpc.Part.Builder builder = org.a2aproject.sdk.grpc.Part.newBuilder();

        if (domain instanceof TextPart textPart) {
            builder.setText(textPart.text());
            builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(textPart.metadata()));
        } else if (domain instanceof FilePart filePart) {
            FileContent fileContent = filePart.file();

            if (fileContent instanceof FileWithBytes fileWithBytes) {
                // Map to raw (bytes), filename, and media_type
                builder.setRaw(ByteString.copyFrom(Base64.getDecoder().decode(fileWithBytes.bytes())));
                if (fileWithBytes.name() != null) {
                    builder.setFilename(fileWithBytes.name());
                }
                if (fileWithBytes.mimeType() != null) {
                    builder.setMediaType(fileWithBytes.mimeType());
                }
            } else if (fileContent instanceof FileWithUri fileWithUri) {
                // Map to url, filename, and media_type
                builder.setUrl(fileWithUri.uri());
                if (fileWithUri.name() != null) {
                    builder.setFilename(fileWithUri.name());
                }
                if (fileWithUri.mimeType() != null) {
                    builder.setMediaType(fileWithUri.mimeType());
                }
            }
            builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(filePart.metadata()));
        } else if (domain instanceof DataPart dataPart) {
            // Map data to google.protobuf.Value (supports object, array, primitive, or null)
            Value dataValue = A2ACommonFieldMapper.INSTANCE.objectToValue(dataPart.data());
            builder.setData(dataValue);
            builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(dataPart.metadata()));
        }

        return builder.build();
    }

    /**
     * Converts proto Part to domain Part.
     * Reconstructs TextPart, FilePart, or DataPart based on oneof content field.
     */
    default Part<?> fromProto(org.a2aproject.sdk.grpc.Part proto) {
        if (proto == null) {
            return null;
        }
        Map<String, Object> metadata = A2ACommonFieldMapper.INSTANCE.metadataFromProto(proto.getMetadata());
        if (proto.hasText()) {
            return new TextPart(proto.getText(), metadata);
        } else if (proto.hasRaw()) {
            // raw bytes → FilePart(FileWithBytes)
            String bytes = Base64.getEncoder().encodeToString(proto.getRaw().toByteArray());
            String mimeType = proto.getMediaType().isEmpty() ? "" : proto.getMediaType();
            String name = proto.getFilename().isEmpty() ? "" : proto.getFilename();
            return new FilePart(new FileWithBytes(mimeType, name, bytes), metadata);
        } else if (proto.hasUrl()) {
            // url → FilePart(FileWithUri)
            String uri = proto.getUrl();
            String mimeType = proto.getMediaType().isEmpty() ? null : proto.getMediaType();
            String name = proto.getFilename().isEmpty() ? null : proto.getFilename();
            return new FilePart(new FileWithUri(mimeType, name, uri), metadata);
        } else if (proto.hasData()) {
            // data (google.protobuf.Value containing any JSON value) → DataPart
            Value dataValue = proto.getData();
            Object data = A2ACommonFieldMapper.INSTANCE.valueToObject(dataValue);
            return new DataPart(data, metadata);
        }

        throw new InvalidRequestError();
    }
}
