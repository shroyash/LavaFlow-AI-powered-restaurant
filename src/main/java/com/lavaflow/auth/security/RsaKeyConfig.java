package com.lavaflow.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA-2048 key pair from PEM files.
 * REUSED from College Bridge RSAKeyConfig — pure Java security infrastructure.
 *
 * Key generation:
 *   openssl genrsa -out raw.pem 2048
 *   openssl pkcs8 -topk8 -nocrypt -in raw.pem -out private_key.pem
 *   openssl rsa -in raw.pem -pubout -out public_key.pem
 */
@Configuration
public class RsaKeyConfig {

    @Value("${app.jwt.privateKeyPath}")
    private Resource privateKeyResource;

    @Value("${app.jwt.publicKeyPath}")
    private Resource publicKeyResource;

    @Bean
    public PrivateKey privateKey() throws Exception {
        String pem = readPem(privateKeyResource);
        String clean = pem
                .replaceAll("-----BEGIN (.*)PRIVATE KEY-----", "")
                .replaceAll("-----END (.*)PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    @Bean
    public PublicKey publicKey() throws Exception {
        String pem = readPem(publicKeyResource);
        String clean = pem
                .replaceAll("-----BEGIN (.*)PUBLIC KEY-----", "")
                .replaceAll("-----END (.*)PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String readPem(Resource resource) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
