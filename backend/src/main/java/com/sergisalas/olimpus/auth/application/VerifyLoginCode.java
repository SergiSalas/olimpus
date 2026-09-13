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
 * Use case: enter the code and stay logged in.
 *
 * <p>This is where the account is born, if it did not exist. The code is spent
 * when used, right or wrong: otherwise a valid code would work forever.
 */
public class VerifyLoginCode {

    /** What goes back to the phone: the token in clear, which is only seen once. */
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
                        .orElseThrow(() -> new InvalidLoginCodeException("no pending code"));

        if (stored.hasExpired(now)) {
            codes.deleteByEmail(email);
            throw new InvalidLoginCodeException("code expired");
        }

        if (!stored.matches(hasher.hash(rawCode == null ? "" : rawCode.trim()))) {
            recordFailure(stored, email);
            throw new InvalidLoginCodeException("code does not match");
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

    /** When the attempts run out the code disappears: a new one must be requested. */
    private void recordFailure(LoginCode stored, EmailAddress email) {
        LoginCode after = stored.afterFailedAttempt();
        if (after.outOfAttempts()) {
            codes.deleteByEmail(email);
        } else {
            codes.save(after);
        }
    }
}
