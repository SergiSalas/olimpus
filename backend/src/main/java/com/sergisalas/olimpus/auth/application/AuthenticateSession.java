package com.sergisalas.olimpus.auth.application;

import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.Session;
import com.sergisalas.olimpus.auth.domain.SessionRepository;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;
import java.util.Optional;

/**
 * Caso de uso: saber quien viene en cada peticion, a partir de la llave que
 * manda el movil. Si la sesion caduco o fue anulada, no hay nadie.
 */
public class AuthenticateSession {

    private final SessionRepository sessions;
    private final AccountRepository accounts;
    private final Hasher hasher;
    private final Clock clock;

    public AuthenticateSession(
            SessionRepository sessions, AccountRepository accounts, Hasher hasher, Clock clock) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.hasher = hasher;
        this.clock = clock;
    }

    public Optional<Account> execute(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return sessions
                .findByTokenHash(hasher.hash(token.trim()))
                .filter(session -> session.isActive(clock.instant()))
                .map(Session::accountId)
                .flatMap(accounts::findById);
    }
}
