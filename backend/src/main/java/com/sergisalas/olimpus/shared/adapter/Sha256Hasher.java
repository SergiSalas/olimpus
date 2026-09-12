package com.sergisalas.olimpus.shared.adapter;

import com.sergisalas.olimpus.shared.domain.Hasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * SHA-256 y ya. Vale para secretos largos y aleatorios como estos, que no se
 * pueden adivinar probando; para contrasenas elegidas por personas habria que
 * usar algo lento a proposito (bcrypt o argon2), pero aqui no hay contrasenas.
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
            throw new IllegalStateException("este Java no trae SHA-256", e);
        }
    }
}
