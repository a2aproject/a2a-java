package io.a2a.server.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCardSignature;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for signing and verifying AgentCards.
 *
 * @author Sandeep Belgavi
 */
@ApplicationScoped
public class SigningService {

    private final DefaultJWSSignerFactory signerFactory = new DefaultJWSSignerFactory();
    private final DefaultJWSVerifierFactory verifierFactory = new DefaultJWSVerifierFactory();

    /**
     * Signs an AgentCard using the provided private key.
     *
     * @param card       the AgentCard to sign
     * @param privateKey the private key (JWK) to use for signing
     * @return a new AgentCard instance with the signature added
     * @throws JOSEException if signing fails
     */
    public AgentCard sign(AgentCard card, JWK privateKey) throws JOSEException {
        // 1. Create a version of the card without signatures for signing
        AgentCard cardToSign = AgentCard.builder(card).signatures(null).build();
        String jsonPayload;
        try {
            jsonPayload = JsonUtil.toJson(cardToSign);
        } catch (JsonProcessingException e) {
            throw new A2AServerException("Failed to serialize AgentCard to JSON", e);
        }

        // 2. Determine the algorithm
        JWSAlgorithm alg = privateKey.getAlgorithm() != null ? 
                JWSAlgorithm.parse(privateKey.getAlgorithm().getName()) : null;
        
        if (alg == null) {
            // Default algorithms based on key type if not specified
            if (com.nimbusds.jose.jwk.KeyType.RSA.equals(privateKey.getKeyType())) {
                alg = JWSAlgorithm.RS256;
            } else if (com.nimbusds.jose.jwk.KeyType.EC.equals(privateKey.getKeyType())) {
                alg = JWSAlgorithm.ES256;
            } else if (com.nimbusds.jose.jwk.KeyType.OKP.equals(privateKey.getKeyType())) {
                alg = JWSAlgorithm.EdDSA;
            } else {
                throw new JOSEException("Unsupported key type for signing: " + privateKey.getKeyType());
            }
        }

        // 3. Create the signer and header
        JWSSigner signer = signerFactory.createJWSSigner(privateKey, alg);
        JWSHeader header = new JWSHeader.Builder(alg)
                .keyID(privateKey.getKeyID())
                .build();

        // 4. Create and sign the JWS object
        JWSObject jwsObject = new JWSObject(header, new Payload(jsonPayload));
        jwsObject.sign(signer);

        // 5. Create the AgentCardSignature
        AgentCardSignature signature = AgentCardSignature.builder()
                .protectedHeader(jwsObject.getHeader().toBase64URL().toString())
                .signature(jwsObject.getSignature().toString())
                .build();

        // 6. Return a new AgentCard with the added signature
        List<AgentCardSignature> signatures = new ArrayList<>();
        if (card.signatures() != null) {
            signatures.addAll(card.signatures());
        }
        signatures.add(signature);

        return AgentCard.builder(card).signatures(signatures).build();
    }

    /**
     * Verifies the signatures on an AgentCard using the provided public key.
     *
     * @param card      the AgentCard to verify
     * @param publicKey the public key (JWK) to use for verification
     * @return true if at least one signature is valid, false otherwise
     * @throws JOSEException  if verification process fails
     * @throws ParseException if signature header parsing fails
     */
    public boolean verify(AgentCard card, JWK publicKey) throws JOSEException, ParseException {
        if (card.signatures() == null || card.signatures().isEmpty()) {
            return false;
        }

        // 1. Reconstruct the payload (card without signatures)
        AgentCard cardWithoutSignatures = AgentCard.builder(card).signatures(null).build();
        String jsonPayload;
        try {
            jsonPayload = JsonUtil.toJson(cardWithoutSignatures);
        } catch (JsonProcessingException e) {
            throw new A2AServerException("Failed to serialize AgentCard to JSON", e);
        }
        Payload payload = new Payload(jsonPayload);

        // 2. Verify each signature
        for (AgentCardSignature sig : card.signatures()) {
            Base64URL protectedHeader = new Base64URL(sig.protectedHeader());
            Base64URL signature = new Base64URL(sig.signature());

            // Reconstruct JWSObject
            JWSObject jwsObject = new JWSObject(protectedHeader, payload, signature);
            
            // Create verifier
            JWSVerifier verifier = verifierFactory.createJWSVerifier(jwsObject.getHeader(), SecurityUtils.toPublicKey(publicKey));
            
            if (jwsObject.verify(verifier)) {
                return true;
            }
        }

        return false;
    }
}
