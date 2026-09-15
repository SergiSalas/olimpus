package com.sergisalas.olimpus.auth.adapter.in;

import com.sergisalas.olimpus.auth.domain.Account;
import java.time.Instant;
import java.util.UUID;
import com.sergisalas.olimpus.auth.application.DeleteAccount;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Who am I. The phone uses it to know whether the token it keeps is still valid. */
@RestController
@RequestMapping("/api")
public class MeController {

    private final DeleteAccount deleteAccount;

    public MeController(DeleteAccount deleteAccount) {
        this.deleteAccount = deleteAccount;
    }

    public record MeResponse(UUID accountId, String email, Instant createdAt) {}

    /**
     * Deleting the account. No grace period and no "we keep it for 30 days in
     * case you change your mind": somebody who wants out is out.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@CurrentAccount Account account) {
        deleteAccount.execute(account.id());
    }

    @GetMapping("/me")
    public MeResponse me(@CurrentAccount Account account) {
        return new MeResponse(account.id(), account.email().value(), account.createdAt());
    }
}
