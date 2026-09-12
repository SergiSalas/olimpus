package com.sergisalas.olimpus.auth.adapter.in;

import com.sergisalas.olimpus.auth.application.RequestLoginCode;
import com.sergisalas.olimpus.auth.application.VerifyLoginCode;
import com.sergisalas.olimpus.auth.domain.Account;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RequestLoginCode requestLoginCode;
    private final VerifyLoginCode verifyLoginCode;

    public AuthController(RequestLoginCode requestLoginCode, VerifyLoginCode verifyLoginCode) {
        this.requestLoginCode = requestLoginCode;
        this.verifyLoginCode = verifyLoginCode;
    }

    public record CodeRequest(String email) {}

    public record VerifyRequest(String email, String code) {}

    public record SessionResponse(
            String token, Instant expiresAt, UUID accountId, String email, boolean isNewAccount) {}

    /**
     * Responde 204 siempre, exista o no la cuenta: asi la app no sirve para
     * averiguar quien esta registrado.
     */
    @PostMapping("/code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestCode(@RequestBody CodeRequest body) {
        requestLoginCode.execute(body.email());
    }

    @PostMapping("/verify")
    public SessionResponse verify(@RequestBody VerifyRequest body) {
        VerifyLoginCode.StartedSession started =
                verifyLoginCode.execute(body.email(), body.code());
        Account account = started.account();
        return new SessionResponse(
                started.token(),
                started.expiresAt(),
                account.id(),
                account.email().value(),
                started.isNew());
    }
}
