package com.sergisalas.olimpus.shared.adapter;

import com.sergisalas.olimpus.shared.domain.Hasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Plain SHA-256. It is enough for long random secrets like these, which cannot
 * be guessed by trial; passwords chosen by people would need something slow on
 * purpose (bcrypt or argon2), but there are no passwords here.
 */
@Component
public class Sha256Hasher implements Hasher {

    @Override
    public String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("this JVM does not ship SHA-256", e);
        }
    }
}
