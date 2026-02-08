package io.a2a.server.security;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SigningService.
 */
class SigningServiceTest {

    private SigningService signingService;

    @BeforeEach
    void setUp() {
        signingService = new SigningService();
    }

    private AgentCard.Builder createTestAgentCardBuilder() {
        return AgentCard.builder()
                .name("Test Agent")
                .description("Test Description")
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.emptyList());
    }

    @Test
    void testSignAndVerifyRSA() throws Exception {
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .keyUse(KeyUse.SIGNATURE)
                .generate();
        
        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();

        AgentCard card = createTestAgentCardBuilder().build();
        
        AgentCard signedCard = signingService.sign(card, rsaJWK);
        
        assertNotNull(signedCard.signatures());
        assertEquals(1, signedCard.signatures().size());
        
        boolean verified = signingService.verify(signedCard, rsaPublicJWK);
        assertTrue(verified, "Signature should be verified");
    }

    @Test
    void testSignAndVerifyEC() throws Exception {
        ECKey ecJWK = new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .generate();
        
        ECKey ecPublicJWK = ecJWK.toPublicJWK();

        AgentCard card = createTestAgentCardBuilder().build();
        
        AgentCard signedCard = signingService.sign(card, ecJWK);
        
        assertNotNull(signedCard.signatures());
        assertEquals(1, signedCard.signatures().size());
        
        boolean verified = signingService.verify(signedCard, ecPublicJWK);
        assertTrue(verified, "Signature should be verified");
    }

    @Test
    void testVerifyFailsWithModifiedCard() throws Exception {
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();
        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();

        AgentCard card = createTestAgentCardBuilder().build();
        AgentCard signedCard = signingService.sign(card, rsaJWK);
        
        // Modify the card content (e.g., description)
        AgentCard modifiedCard = AgentCard.builder(signedCard)
                .description("Modified Description")
                .build();
        
        boolean verified = signingService.verify(modifiedCard, rsaPublicJWK);
        assertFalse(verified, "Verification should fail for modified card");
    }

    @Test
    void testVerifyFailsWithWrongKey() throws Exception {
        RSAKey rsaJWK1 = new RSAKeyGenerator(2048).generate();
        RSAKey rsaJWK2 = new RSAKeyGenerator(2048).generate();
        
        AgentCard card = createTestAgentCardBuilder().build();
        AgentCard signedCard = signingService.sign(card, rsaJWK1);
        
        boolean verified = signingService.verify(signedCard, rsaJWK2.toPublicJWK());
        assertFalse(verified, "Verification should fail with wrong key");
    }
}
