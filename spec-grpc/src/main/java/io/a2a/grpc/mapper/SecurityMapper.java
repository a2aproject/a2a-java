package io.a2a.grpc.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ProtocolStringList;
import io.a2a.grpc.SecurityRequirement;
import io.a2a.grpc.StringList;
import org.mapstruct.Mapper;

/**
 * Mapper between domain security requirements and protobuf SecurityRequirement messages.
 * <p>
 * Domain representation: {@code List<Map<String, List<String>>>} where each map represents
 * one security option with scheme names as keys and scopes as values.
 * <p>
 * Proto representation: {@code repeated SecurityRequirement} where each SecurityRequirement has
 * {@code map<string, StringList> schemes}.
 * <p>
 * Example: A security requirement that allows either OAuth2 with read/write scopes OR API Key:
 * <pre>
 * Domain: [
 *   {"oauth2": ["read", "write"]},
 *   {"apiKey": []}
 * ]
 * Proto: [
 *   SecurityRequirement{schemes: {"oauth2": StringList{values: ["read", "write"]}}},
 *   SecurityRequirement{schemes: {"apiKey": StringList{values: []}}}
 * ]
 * </pre>
 * <p>
 * <b>Manual Implementation Required:</b> Handles complex nested structure ({@code List<Map<String, List<String>>>} ↔
 * {@code repeated SecurityRequirement} with {@code map<string, StringList>}) requiring manual iteration and StringList wrapper handling.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface SecurityMapper {

    SecurityMapper INSTANCE = A2AMappers.getMapper(SecurityMapper.class);

    /**
     * Converts a single domain security requirement map to a proto SecurityRequirement message.
     * <p>
     * MapStruct will call this method for each element when mapping the list.
     *
     * @param schemeMap map of scheme names to scopes
     * @return SecurityRequirement proto message, or null if input is null
     */
    default SecurityRequirement mapSecurityItem(Map<String, List<String>> schemeMap) {
        if (schemeMap == null) {
            return null;
        }

        SecurityRequirement.Builder securityBuilder = SecurityRequirement.newBuilder();
        for (Map.Entry<String, List<String>> entry : schemeMap.entrySet()) {
            StringList.Builder stringListBuilder = StringList.newBuilder();
            if (entry.getValue() != null) {
                stringListBuilder.addAllList(entry.getValue());
            }
            securityBuilder.putSchemes(entry.getKey(), stringListBuilder.build());
        }
        return securityBuilder.build();
    }

    /**
     * Converts domain security requirements to proto SecurityRequirement messages.
     * <p>
     * Each Map in the domain list becomes one SecurityRequirement message in proto, representing
     * one way to satisfy the security requirements (OR relationship between list items).
     *
     * @param domainSecurity list of maps representing security requirement options
     * @return list of SecurityRequirement proto messages, or null if input is null
     */
    default List<SecurityRequirement> toProto(List<Map<String, List<String>>> domainSecurity) {
        if (domainSecurity == null) {
            return null;
        }

        List<SecurityRequirement> protoList = new ArrayList<>(domainSecurity.size());
        for (Map<String, List<String>> schemeMap : domainSecurity) {
            protoList.add(mapSecurityItem(schemeMap));
        }
        return protoList;
    }

    /**
     * Converts proto SecurityRequirement messages to domain security requirements.
     *
     * @param protoSecurity list of SecurityRequirement proto messages
     * @return list of maps representing security requirement options, or null if input is null
     */
    default List<Map<String, List<String>>> fromProto(List<SecurityRequirement> protoSecurity) {
        if (protoSecurity == null) {
            return null;
        }

        List<Map<String, List<String>>> domainList = new ArrayList<>(protoSecurity.size());
        for (SecurityRequirement security : protoSecurity) {
            Map<String, List<String>> schemeMap = new LinkedHashMap<>();
            for (Map.Entry<String, StringList> entry : security.getSchemesMap().entrySet()) {
                ProtocolStringList listList = entry.getValue().getListList();
                List<String> values = new ArrayList<>(listList);
                schemeMap.put(entry.getKey(), values);
            }
            domainList.add(schemeMap);
        }
        return domainList;
    }
}
