package com.sergisalas.olimpus.auth.adapter.in;

import com.sergisalas.olimpus.auth.domain.Account;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Quien soy. Sirve al movil para saber si la llave que guarda sigue valiendo. */
@RestController
@RequestMapping("/api")
public class MeController {

    public record MeResponse(UUID accountId, String email, Instant createdAt) {}

    @GetMapping("/me")
    public MeResponse me(@CurrentAccount Account account) {
        return new MeResponse(account.id(), account.email().value(), account.createdAt());
    }
}
