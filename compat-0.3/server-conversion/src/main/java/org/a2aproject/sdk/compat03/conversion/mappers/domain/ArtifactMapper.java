package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Part;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting Artifact between A2A Protocol v0.3 and v1.0.
 * <p>
 * Both versions are records with the same structure:
 * {@code Artifact(artifactId, name, description, parts, metadata, extensions)}.
 * <p>
 * The conversion primarily involves converting the nested {@link Part} list using {@link PartMapper}.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {PartMapper.class})
public interface ArtifactMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    ArtifactMapper INSTANCE = A03Mappers.getMapper(ArtifactMapper.class);

    /**
     * Converts v0.3 Artifact to v1.0 Artifact.
     * <p>
     * Converts all Part instances in the parts list using PartMapper.
     *
     * @param v03 the v0.3 artifact
     * @return the equivalent v1.0 artifact
     */
    default Artifact toV10(org.a2aproject.sdk.compat03.spec.Artifact v03) {
        if (v03 == null) {
            return null;
        }

        List<Part<?>> parts = v03.parts().stream()
            .map(PartMapper.INSTANCE::toV10)
            .collect(Collectors.toList());

        return new Artifact(
            v03.artifactId(),
            v03.name(),
            v03.description(),
            parts,
            v03.metadata(),
            v03.extensions()
        );
    }

    /**
     * Converts v1.0 Artifact to v0.3 Artifact.
     * <p>
     * Converts all Part instances in the parts list using PartMapper.
     *
     * @param v10 the v1.0 artifact
     * @return the equivalent v0.3 artifact
     */
    default org.a2aproject.sdk.compat03.spec.Artifact fromV10(Artifact v10) {
        if (v10 == null) {
            return null;
        }

        List<org.a2aproject.sdk.compat03.spec.Part<?>> parts = v10.parts().stream()
            .map(PartMapper.INSTANCE::fromV10)
            .collect(Collectors.toList());

        return new org.a2aproject.sdk.compat03.spec.Artifact(
            v10.artifactId(),
            v10.name(),
            v10.description(),
            parts,
            v10.metadata(),
            v10.extensions()
        );
    }
}
