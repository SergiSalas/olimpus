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

    private FakeAuthWorld world;
    private AuthenticateSession whoAreYou;
    private Account ana;

    @BeforeEach
    void setUp() {
        world = new FakeAuthWorld();
        whoAreYou =
                new AuthenticateSession(world.sessions, world.accounts, world.hasher, world.clock);
        ana = world.accounts.save(Account.created(EmailAddress.of("ana@example.com"), world.now));
        world.sessions.save(Session.started(world.hasher.hash("my-token"), ana.id(), world.now));
    }

    @Test
    void with_the_right_token_it_knows_who_you_are() {
        assertThat(whoAreYou.execute("my-token")).contains(ana);
    }

    @Test
    void a_made_up_token_is_nobody() {
        assertThat(whoAreYou.execute("made-up-token")).isEmpty();
    }

    @Test
    void no_token_is_nobody() {
        assertThat(whoAreYou.execute(null)).isEmpty();
        assertThat(whoAreYou.execute("   ")).isEmpty();
    }

    @Test
    void after_ninety_days_the_session_is_no_longer_valid() {
        world.now = world.now.plus(Duration.ofDays(90));

        assertThat(whoAreYou.execute("my-token")).isEmpty();
    }

    @Test
    void a_revoked_session_stops_working_immediately() {
        Session live = world.sessions.findByTokenHash(world.hasher.hash("my-token")).orElseThrow();
        world.sessions.save(
                new Session(
                        live.tokenHash(), live.accountId(), live.createdAt(), live.expiresAt(), world.now));

        assertThat(whoAreYou.execute("my-token")).isEmpty();
    }
}
