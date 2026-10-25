package com.demo.upimesh.crypto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Holds the server's RSA keypair.
 *
 * - If UPI_RSA_PRIVATE_KEY (base64 PKCS#8 DER) is set, that key is loaded and the
 *   public key is derived from it. The key is therefore stable across restarts and
 *   across instances, so packets encrypted before a restart can still be decrypted.
 * - Otherwise (dev/test only) a fresh keypair is generated at startup.
 * - If upi.mesh.rsa.require-persistent-key=true (prod profile) and no key is
 *   provided, startup FAILS instead of silently generating a throwaway key.
 *
 * Generate a key with:  java scripts/GenerateRsaKey.java
 * In a real bank this key would live in an HSM / KMS, not in an env var.
 */
@Component
public class ServerKeyHolder {

    private static final Logger log = LoggerFactory.getLogger(ServerKeyHolder.class);

    @Value("${upi.mesh.rsa.private-key:}")
    private String privateKeyBase64;

    @Value("${upi.mesh.rsa.require-persistent-key:false}")
    private boolean requirePersistentKey;

    private KeyPair keyPair;

    @PostConstruct
    public void init() throws Exception {
        if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
            this.keyPair = loadFromPrivateKey(privateKeyBase64);
            log.info("Server RSA keypair LOADED from configuration. Public key fingerprint: {}",
                    getPublicKeyBase64().substring(0, 32) + "...");
            return;
        }
        if (requirePersistentKey) {
            throw new IllegalStateException(
                    "UPI_RSA_PRIVATE_KEY is not set but a persistent key is required. "
                  + "Generate one with: java scripts/GenerateRsaKey.java");
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        this.keyPair = gen.generateKeyPair();
        log.warn("No UPI_RSA_PRIVATE_KEY set - generated a TEMPORARY 2048-bit keypair "
                + "(lost on restart). Public key fingerprint: {}",
                getPublicKeyBase64().substring(0, 32) + "...");
    }

    private static KeyPair loadFromPrivateKey(String base64) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(base64.trim());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(der));
        if (!(priv instanceof RSAPrivateCrtKey crt)) {
            throw new IllegalStateException("UPI_RSA_PRIVATE_KEY must be an RSA CRT private key (PKCS#8)");
        }
        PublicKey pub = kf.generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
        return new KeyPair(pub, priv);
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
