package com.sergisalas.olimpus.auth.application;

import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.auth.domain.InvalidLoginCodeException;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import com.sergisalas.olimpus.auth.domain.Secrets;
import com.sergisalas.olimpus.auth.domain.Session;
import com.sergisalas.olimpus.auth.domain.SessionRepository;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso: meter el codigo y quedarse dentro.
 *
 * <p>Aqui nace la cuenta, si no existia. El codigo se gasta al usarlo, acierte
 * o no: sin eso, un codigo valido serviria para siempre.
 */
public class VerifyLoginCode {

    /** Lo que se devuelve al movil: la llave en claro, que solo se ve una vez. */
    public record StartedSession(String token, Instant expiresAt, Account account, boolean isNew) {}

    private final LoginCodeRepository codes;
    private final AccountRepository accounts;
    private final SessionRepository sessions;
    private final Secrets secrets;
    private final Hasher hasher;
    private final Clock clock;

    public VerifyLoginCode(
            LoginCodeRepository codes,
            AccountRepository accounts,
            SessionRepository sessions,
            Secrets secrets,
            Hasher hasher,
            Clock clock) {
        this.codes = codes;
        this.accounts = accounts;
        this.sessions = sessions;
        this.secrets = secrets;
        this.hasher = hasher;
        this.clock = clock;
    }

    public StartedSession execute(String rawEmail, String rawCode) {
        EmailAddress email = EmailAddress.of(rawEmail);
        Instant now = clock.instant();

        LoginCode stored =
                codes.findByEmail(email)
                        .orElseThrow(() -> new InvalidLoginCodeException("no hay codigo pendiente"));

        if (stored.hasExpired(now)) {
            codes.deleteByEmail(email);
            throw new InvalidLoginCodeException("el codigo ha caducado");
        }

        if (!stored.matches(hasher.hash(rawCode == null ? "" : rawCode.trim()))) {
            registrarFallo(stored, email);
            throw new InvalidLoginCodeException("el codigo no coincide");
        }

        codes.deleteByEmail(email);

        boolean isNew = accounts.findByEmail(email).isEmpty();
        Account account =
                accounts.findByEmail(email).orElseGet(() -> accounts.save(Account.created(email, now)));

        String token = secrets.sessionToken();
        Session session = Session.started(hasher.hash(token), account.id(), now);
        sessions.save(session);

        return new StartedSession(token, session.expiresAt(), account, isNew);
    }

    /** Cuando se agotan los intentos el codigo desaparece: hay que pedir otro. */
    private void registrarFallo(LoginCode stored, EmailAddress email) {
        LoginCode after = stored.afterFailedAttempt();
        if (after.outOfAttempts()) {
            codes.deleteByEmail(email);
        } else {
            codes.save(after);
        }
    }
}
