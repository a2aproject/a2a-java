package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Part;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting Artifact between A2A Protocol v0.3 and v1.0.
 * <p>
 * Both versions are records with the same structure:
 * {@code Artifact(artifactId, name, description, parts, metadata, extensions)}.
 * <p>
 * The conversion primarily involves converting the nested {@link Part} list using {@link PartMapper_v0_3}.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {PartMapper_v0_3.class})
public interface ArtifactMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    ArtifactMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(ArtifactMapper_v0_3.class);

    /**
     * Converts v0.3 Artifact to v1.0 Artifact.
     * <p>
     * Converts all Part instances in the parts list using PartMapper.
     *
     * @param v03 the v0.3 artifact
     * @return the equivalent v1.0 artifact
     */
    default Artifact toV10(Artifact_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        List<Part<?>> parts = v03.parts().stream()
            .map(PartMapper_v0_3.INSTANCE::toV10)
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
    default Artifact_v0_3 fromV10(Artifact v10) {
        if (v10 == null) {
            return null;
        }

        List<Part_v0_3<?>> parts = v10.parts().stream()
            .map(PartMapper_v0_3.INSTANCE::fromV10)
            .collect(Collectors.toList());

        return new Artifact_v0_3(
            v10.artifactId(),
            v10.name(),
            v10.description(),
            parts,
            v10.metadata(),
            v10.extensions()
        );
    }
}
