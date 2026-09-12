package com.sergisalas.olimpus.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.auth.FakeAuthWorld;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.auth.domain.Session;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticateSessionTest {

    private FakeAuthWorld mundo;
    private AuthenticateSession quienEres;
    private Account ana;

    @BeforeEach
    void setUp() {
        mundo = new FakeAuthWorld();
        quienEres =
                new AuthenticateSession(mundo.sessions, mundo.accounts, mundo.hasher, mundo.clock);
        ana = mundo.accounts.save(Account.created(EmailAddress.of("ana@example.com"), mundo.now));
        mundo.sessions.save(Session.started(mundo.hasher.hash("mi-llave"), ana.id(), mundo.now));
    }

    @Test
    void con_la_llave_buena_sabe_quien_eres() {
        assertThat(quienEres.execute("mi-llave")).contains(ana);
    }

    @Test
    void una_llave_inventada_no_es_nadie() {
        assertThat(quienEres.execute("llave-inventada")).isEmpty();
    }

    @Test
    void sin_llave_no_es_nadie() {
        assertThat(quienEres.execute(null)).isEmpty();
        assertThat(quienEres.execute("   ")).isEmpty();
    }

    @Test
    void a_los_noventa_dias_la_sesion_ya_no_vale() {
        mundo.now = mundo.now.plus(Duration.ofDays(90));

        assertThat(quienEres.execute("mi-llave")).isEmpty();
    }

    @Test
    void una_sesion_anulada_deja_de_valer_al_momento() {
        Session viva = mundo.sessions.findByTokenHash(mundo.hasher.hash("mi-llave")).orElseThrow();
        mundo.sessions.save(
                new Session(
                        viva.tokenHash(), viva.accountId(), viva.createdAt(), viva.expiresAt(), mundo.now));

        assertThat(quienEres.execute("mi-llave")).isEmpty();
    }
}
