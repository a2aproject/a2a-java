package io.a2a.server.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Utility class for security-related operations, including key conversion.
 *
 * @author Sandeep Belgavi
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts a JWK to a Java PrivateKey.
     *
     * @param jwk the JSON Web Key
     * @return the PrivateKey
     * @throws JOSEException if conversion fails or key type is unsupported
     */
    public static PrivateKey toPrivateKey(JWK jwk) throws JOSEException {
        if (jwk instanceof RSAKey rsaKey) {
            return rsaKey.toPrivateKey();
        } else if (jwk instanceof ECKey ecKey) {
            return ecKey.toPrivateKey();
        } else if (jwk instanceof OctetKeyPair okpKey) {
            return okpKey.toPrivateKey();
        }
        throw new JOSEException("Unsupported JWK type: " + jwk.getKeyType());
    }

    /**
     * Converts a JWK to a Java PublicKey.
     *
     * @param jwk the JSON Web Key
     * @return the PublicKey
     * @throws JOSEException if conversion fails or key type is unsupported
     */
    public static PublicKey toPublicKey(JWK jwk) throws JOSEException {
        if (jwk instanceof RSAKey rsaKey) {
            return rsaKey.toPublicKey();
        } else if (jwk instanceof ECKey ecKey) {
            return ecKey.toPublicKey();
        } else if (jwk instanceof OctetKeyPair okpKey) {
            return okpKey.toPublicKey();
        }
        throw new JOSEException("Unsupported JWK type: " + jwk.getKeyType());
    }
}
