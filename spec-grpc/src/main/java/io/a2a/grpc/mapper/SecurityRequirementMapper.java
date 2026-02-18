package io.a2a.grpc.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ProtocolStringList;
import io.a2a.grpc.StringList;
import org.mapstruct.Mapper;

/**
 * Mapper between domain security requirements and protobuf SecurityRequirement messages.
 * <p>
 * Domain representation: {@code List<SecurityRequirement>} where each SecurityRequirement contains
 * a schemes map with scheme names as keys and scopes as values.
 * <p>
 * Proto representation: {@code repeated SecurityRequirement} where each SecurityRequirement has
 * {@code map<string, StringList> schemes}.
 * <p>
 * Example: A security requirement that allows either OAuth2 with read/write scopes OR API Key:
 * <pre>
 * Domain: [
 *   SecurityRequirement{schemes: {"oauth2": ["read", "write"]}},
 *   SecurityRequirement{schemes: {"apiKey": []}}
 * ]
 * Proto: [
 *   SecurityRequirement{schemes: {"oauth2": StringList{values: ["read", "write"]}}},
 *   SecurityRequirement{schemes: {"apiKey": StringList{values: []}}}
 * ]
 * </pre>
 * <p>
 * <b>Manual Implementation Required:</b> Handles complex nested structure ({@code List<SecurityRequirement>} ↔
 * {@code repeated SecurityRequirement} with {@code map<string, StringList>}) requiring manual iteration and StringList wrapper handling.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface SecurityRequirementMapper {

    SecurityRequirementMapper INSTANCE = A2AMappers.getMapper(SecurityRequirementMapper.class);

    /**
     * Converts a single domain SecurityRequirement to a proto SecurityRequirement message.
     * <p>
     * MapStruct will call this method for each element when mapping the list.
     *
     * @param domainRequirement domain SecurityRequirement with schemes map
     * @return SecurityRequirement proto message, or null if input is null
     */
    default io.a2a.grpc.SecurityRequirement mapSecurityRequirement(io.a2a.spec.SecurityRequirement domainRequirement) {
        if (domainRequirement == null) {
            return null;
        }

        io.a2a.grpc.SecurityRequirement.Builder securityBuilder = io.a2a.grpc.SecurityRequirement.newBuilder();
        Map<String, List<String>> schemes = domainRequirement.schemes();
        if (schemes != null) {
            for (Map.Entry<String, List<String>> entry : schemes.entrySet()) {
                StringList.Builder stringListBuilder = StringList.newBuilder();
                if (entry.getValue() != null) {
                    stringListBuilder.addAllList(entry.getValue());
                }
                securityBuilder.putSchemes(entry.getKey(), stringListBuilder.build());
            }
        }
        return securityBuilder.build();
    }

    /**
     * Converts domain security requirements to proto SecurityRequirement messages.
     * <p>
     * Each SecurityRequirement in the domain list becomes one SecurityRequirement message in proto,
     * representing one way to satisfy the security requirements (OR relationship between list items).
     *
     * @param domainSecurity list of SecurityRequirement domain objects
     * @return list of SecurityRequirement proto messages, or null if input is null
     */
    default List<io.a2a.grpc.SecurityRequirement> toProto(List<io.a2a.spec.SecurityRequirement> domainSecurity) {
        if (domainSecurity == null) {
            return null;
        }

        List<io.a2a.grpc.SecurityRequirement> protoList = new ArrayList<>(domainSecurity.size());
        for (io.a2a.spec.SecurityRequirement requirement : domainSecurity) {
            protoList.add(mapSecurityRequirement(requirement));
        }
        return protoList;
    }

    /**
     * Converts proto SecurityRequirement messages to domain security requirements.
     *
     * @param protoSecurity list of SecurityRequirement proto messages
     * @return list of SecurityRequirement domain objects, or null if input is null
     */
    default List<io.a2a.spec.SecurityRequirement> fromProto(List<io.a2a.grpc.SecurityRequirement> protoSecurity) {
        if (protoSecurity == null) {
            return null;
        }

        List<io.a2a.spec.SecurityRequirement> domainList = new ArrayList<>(protoSecurity.size());
        for (io.a2a.grpc.SecurityRequirement security : protoSecurity) {
            Map<String, List<String>> schemeMap = new LinkedHashMap<>();
            for (Map.Entry<String, StringList> entry : security.getSchemesMap().entrySet()) {
                ProtocolStringList listList = entry.getValue().getListList();
                List<String> values = new ArrayList<>(listList);
                schemeMap.put(entry.getKey(), values);
            }
            domainList.add(new io.a2a.spec.SecurityRequirement(schemeMap));
        }
        return domainList;
    }
}
