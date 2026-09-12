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
 * Un mundo de juguete para los tests: memoria en vez de base de datos, un
 * reloj que se mueve a mano y secretos predecibles. Sin Spring y sin red.
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

    /** Huella de juguete, pero cumple lo que importa: distinta para cada entrada. */
    public final Hasher hasher = secret -> "huella(" + secret + ")";

    public String nextCode = "123456";
    public String nextToken = "llave-de-sesion";

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

    public final List<String> enviados = new ArrayList<>();

    public final CodeSender sender =
            (email, code) -> enviados.add(email.value() + ":" + code);

    public final Map<String, LoginCode> codigos = new HashMap<>();

    public final LoginCodeRepository codes =
            new LoginCodeRepository() {
                @Override
                public Optional<LoginCode> findByEmail(EmailAddress email) {
                    return Optional.ofNullable(codigos.get(email.value()));
                }

                @Override
                public void save(LoginCode code) {
                    codigos.put(code.email().value(), code);
                }

                @Override
                public void deleteByEmail(EmailAddress email) {
                    codigos.remove(email.value());
                }
            };

    public final Map<String, Account> cuentas = new HashMap<>();

    public final AccountRepository accounts =
            new AccountRepository() {
                @Override
                public Optional<Account> findByEmail(EmailAddress email) {
                    return Optional.ofNullable(cuentas.get(email.value()));
                }

                @Override
                public Optional<Account> findById(UUID id) {
                    return cuentas.values().stream().filter(a -> a.id().equals(id)).findFirst();
                }

                @Override
                public Account save(Account account) {
                    cuentas.put(account.email().value(), account);
                    return account;
                }
            };

    public final Map<String, Session> sesiones = new HashMap<>();

    public final SessionRepository sessions =
            new SessionRepository() {
                @Override
                public Optional<Session> findByTokenHash(String tokenHash) {
                    return Optional.ofNullable(sesiones.get(tokenHash));
                }

                @Override
                public void save(Session session) {
                    sesiones.put(session.tokenHash(), session);
                }
            };
}
