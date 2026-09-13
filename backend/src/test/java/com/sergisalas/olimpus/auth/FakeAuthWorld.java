package com.sergisalas.olimpus.auth;

import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.CodeSender;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import com.sergisalas.olimpus.auth.domain.Secrets;
import com.sergisalas.olimpus.auth.domain.Session;
import com.sergisalas.olimpus.auth.domain.SessionRepository;
import com.sergisalas.olimpus.shared.domain.Hasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A toy world for tests: memory instead of a database, a clock moved by hand
 * and predictable secrets. No Spring and no network.
 */
public class FakeAuthWorld {

    public Instant now = Instant.parse("2026-09-12T10:00:00Z");

    public final Clock clock =
            new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return now;
                }
            };

    /** A toy hash, but it keeps what matters: different for every input. */
    public final Hasher hasher = secret -> "hash(" + secret + ")";

    public String nextCode = "123456";
    public String nextToken = "session-token";

    public final Secrets secrets =
            new Secrets() {
                @Override
                public String sixDigitCode() {
                    return nextCode;
                }

                @Override
                public String sessionToken() {
                    return nextToken;
                }
            };

    public final List<String> sent = new ArrayList<>();

    public final CodeSender sender = (email, code) -> sent.add(email.value() + ":" + code);

    public final Map<String, LoginCode> storedCodes = new HashMap<>();

    public final LoginCodeRepository codes =
            new LoginCodeRepository() {
                @Override
                public Optional<LoginCode> findByEmail(EmailAddress email) {
                    return Optional.ofNullable(storedCodes.get(email.value()));
                }

                @Override
                public void save(LoginCode code) {
                    storedCodes.put(code.email().value(), code);
                }

                @Override
                public void deleteByEmail(EmailAddress email) {
                    storedCodes.remove(email.value());
                }
            };

    public final Map<String, Account> storedAccounts = new HashMap<>();

    public final AccountRepository accounts =
            new AccountRepository() {
                @Override
                public Optional<Account> findByEmail(EmailAddress email) {
                    return Optional.ofNullable(storedAccounts.get(email.value()));
                }

                @Override
                public Optional<Account> findById(UUID id) {
                    return storedAccounts.values().stream().filter(a -> a.id().equals(id)).findFirst();
                }

                @Override
                public Account save(Account account) {
                    storedAccounts.put(account.email().value(), account);
                    return account;
                }
            };

    public final Map<String, Session> storedSessions = new HashMap<>();

    public final SessionRepository sessions =
            new SessionRepository() {
                @Override
                public Optional<Session> findByTokenHash(String tokenHash) {
                    return Optional.ofNullable(storedSessions.get(tokenHash));
                }

                @Override
                public void save(Session session) {
                    storedSessions.put(session.tokenHash(), session);
                }
            };
}
