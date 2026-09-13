package com.sergisalas.olimpus.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.auth.FakeAuthWorld;
import com.sergisalas.olimpus.auth.domain.InvalidLoginCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifyLoginCodeTest {

    private FakeAuthWorld world;
    private RequestLoginCode requestCode;
    private VerifyLoginCode enterCode;

    @BeforeEach
    void setUp() {
        world = new FakeAuthWorld();
        requestCode =
                new RequestLoginCode(
                        world.codes, world.secrets, world.hasher, world.sender, world.clock);
        enterCode =
                new VerifyLoginCode(
                        world.codes,
                        world.accounts,
                        world.sessions,
                        world.secrets,
                        world.hasher,
                        world.clock);
    }

    @Test
    void with_the_right_code_the_account_is_born_and_the_session_starts() {
        world.nextCode = "123456";
        requestCode.execute("ana@example.com");

        VerifyLoginCode.StartedSession session = enterCode.execute("ana@example.com", "123456");

        assertThat(session.isNew()).isTrue();
        assertThat(session.token()).isEqualTo("session-token");
        assertThat(session.expiresAt()).isEqualTo(world.now.plus(java.time.Duration.ofDays(90)));
        assertThat(world.storedAccounts).containsKey("ana@example.com");
        assertThat(world.storedSessions).containsKey("hash(session-token)");
    }

    @Test
    void the_second_time_it_is_the_same_account() {
        requestCode.execute("ana@example.com");
        var first = enterCode.execute("ana@example.com", world.nextCode);

        requestCode.execute("ana@example.com");
        world.nextToken = "another-token";
        var second = enterCode.execute("ana@example.com", world.nextCode);

        assertThat(second.isNew()).isFalse();
        assertThat(second.account().id()).isEqualTo(first.account().id());
        assertThat(world.storedSessions).hasSize(2);
    }

    @Test
    void the_code_is_spent_when_used() {
        requestCode.execute("ana@example.com");
        enterCode.execute("ana@example.com", world.nextCode);

        assertThatThrownBy(() -> enterCode.execute("ana@example.com", world.nextCode))
                .isInstanceOf(InvalidLoginCodeException.class);
    }

    @Test
    void ten_minutes_and_one_second_later_it_is_no_longer_valid() {
        requestCode.execute("ana@example.com");
        world.now = world.now.plusSeconds(601);

        assertThatThrownBy(() -> enterCode.execute("ana@example.com", world.nextCode))
                .isInstanceOf(InvalidLoginCodeException.class);
        assertThat(world.storedCodes).isEmpty();
    }

    @Test
    void each_failure_spends_an_attempt_and_on_the_fifth_the_code_disappears() {
        world.nextCode = "123456";
        requestCode.execute("ana@example.com");

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> enterCode.execute("ana@example.com", "000000"))
                    .isInstanceOf(InvalidLoginCodeException.class);
            assertThat(world.storedCodes.get("ana@example.com").attemptsLeft()).isEqualTo(5 - attempt);
        }

        assertThatThrownBy(() -> enterCode.execute("ana@example.com", "000000"))
                .isInstanceOf(InvalidLoginCodeException.class);
        assertThat(world.storedCodes).isEmpty();

        // And the right code no longer works either: a new one must be requested.
        assertThatThrownBy(() -> enterCode.execute("ana@example.com", "123456"))
                .isInstanceOf(InvalidLoginCodeException.class);
    }

    @Test
    void another_emails_code_does_not_work() {
        world.nextCode = "123456";
        requestCode.execute("ana@example.com");

        assertThatThrownBy(() -> enterCode.execute("carlos@example.com", "123456"))
                .isInstanceOf(InvalidLoginCodeException.class);
    }

    @Test
    void extra_spaces_when_typing_the_code_are_forgiven() {
        world.nextCode = "123456";
        requestCode.execute("ana@example.com");

        assertThat(enterCode.execute("ana@example.com", "  123456 ").account().email().value())
                .isEqualTo("ana@example.com");
    }
}
