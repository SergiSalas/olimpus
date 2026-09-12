package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.Secrets;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class SecureRandomSecrets implements Secrets {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String sixDigitCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    @Override
    public String sessionToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
