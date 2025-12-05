package io.a2a.grpc.mapper;


import com.google.protobuf.ByteString;
import io.a2a.spec.FileContent;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.InvalidRequestError;
import java.util.Base64;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.FilePart} and {@link io.a2a.grpc.FilePart}.
 * <p>
 * Handles polymorphic FileContent (FileWithBytes vs FileWithUri) conversion.
 * <p>
 * <b>Manual Implementation Required:</b> Must use manual default methods to handle protobuf oneof pattern
 * (file_with_bytes vs file_with_uri fields) and ByteString conversion, which MapStruct cannot automatically handle.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface FilePartMapper {

    FilePartMapper INSTANCE = A2AMappers.getMapper(FilePartMapper.class);

    /**
     * Converts domain FilePart to proto FilePart.
     * Handles FileWithBytes and FileWithUri polymorphism.
     */
    default io.a2a.grpc.FilePart toProto(io.a2a.spec.FilePart domain) {
        if (domain == null) {
            return null;
        }

        io.a2a.grpc.FilePart.Builder builder = io.a2a.grpc.FilePart.newBuilder();
        FileContent fileContent = domain.file();

        if (fileContent instanceof FileWithBytes fileWithBytes) {
            builder.setFileWithBytes(ByteString.copyFrom(Base64.getDecoder().decode(fileWithBytes.bytes())));
            if (fileWithBytes.mimeType() != null) {
                builder.setMediaType(fileWithBytes.mimeType());
            }
            if (fileWithBytes.name() != null) {
                builder.setName(fileWithBytes.name());
            }
        } else if (fileContent instanceof FileWithUri fileWithUri) {
            builder.setFileWithUri(fileWithUri.uri());
            if (fileWithUri.mimeType() != null) {
                builder.setMediaType(fileWithUri.mimeType());
            }
            if (fileWithUri.name() != null) {
                builder.setName(fileWithUri.name());
            }
        }

        return builder.build();
    }

    /**
     * Converts proto FilePart to domain FilePart.
     * Reconstructs FileWithBytes or FileWithUri based on oneof field.
     */
    default io.a2a.spec.FilePart fromProto(io.a2a.grpc.FilePart proto) {
        if (proto == null) {
            return null;
        }

        String mimeType = proto.getMediaType().isEmpty() ? null : proto.getMediaType();
        String name = proto.getName().isEmpty() ? null : proto.getName();

        if (proto.hasFileWithBytes()) {
            String bytes = Base64.getEncoder().encodeToString(proto.getFileWithBytes().toByteArray());
            return new io.a2a.spec.FilePart(new FileWithBytes(mimeType, name, bytes));
        } else if (proto.hasFileWithUri()) {
            String uri = proto.getFileWithUri();
            return new io.a2a.spec.FilePart(new FileWithUri(mimeType, name, uri));
        }

        throw new InvalidRequestError();
    }
}
