package com.sergisalas.olimpus.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.auth.FakeAuthWorld;
import com.sergisalas.olimpus.auth.domain.InvalidEmailException;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestLoginCodeTest {

    private FakeAuthWorld world;
    private RequestLoginCode requestCode;

    @BeforeEach
    void setUp() {
        world = new FakeAuthWorld();
        requestCode =
                new RequestLoginCode(
                        world.codes, world.secrets, world.hasher, world.sender, world.clock);
    }

    @Test
    void sends_the_code_to_the_email_and_stores_only_its_hash() {
        world.nextCode = "246810";

        requestCode.execute("Ana@Example.com");

        assertThat(world.sent).containsExactly("ana@example.com:246810");
        // The hash is stored, not the code. That the real hash is irreversible
        // is checked in Sha256HasherTest.
        LoginCode stored = world.storedCodes.get("ana@example.com");
        assertThat(stored.codeHash()).isEqualTo(world.hasher.hash("246810"));
    }

    @Test
    void the_code_expires_after_ten_minutes_and_comes_with_five_attempts() {
        requestCode.execute("ana@example.com");

        LoginCode stored = world.storedCodes.get("ana@example.com");
        assertThat(stored.expiresAt()).isEqualTo(world.now.plusSeconds(600));
        assertThat(stored.attemptsLeft()).isEqualTo(5);
    }

    @Test
    void requesting_another_code_replaces_the_previous_one() {
        world.nextCode = "111111";
        requestCode.execute("ana@example.com");
        world.nextCode = "222222";
        requestCode.execute("ana@example.com");

        assertThat(world.storedCodes).hasSize(1);
        assertThat(world.storedCodes.get("ana@example.com").codeHash()).isEqualTo("hash(222222)");
    }

    @Test
    void requesting_a_code_creates_no_account() {
        requestCode.execute("ana@example.com");

        assertThat(world.storedAccounts).isEmpty();
    }

    @Test
    void a_malformed_email_sends_nothing() {
        assertThatThrownBy(() -> requestCode.execute("ana(at)example.com"))
                .isInstanceOf(InvalidEmailException.class);

        assertThat(world.sent).isEmpty();
    }
}
