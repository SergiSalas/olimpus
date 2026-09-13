package com.sergisalas.olimpus.shared.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256HasherTest {

    private final Sha256Hasher hasher = new Sha256Hasher();

    @Test
    void the_hash_does_not_reveal_the_secret() {
        assertThat(hasher.hash("246810")).doesNotContain("246810");
    }

    @Test
    void the_same_secret_always_gives_the_same_hash() {
        assertThat(hasher.hash("246810")).isEqualTo(hasher.hash("246810"));
    }

    @Test
    void two_similar_secrets_give_different_hashes() {
        assertThat(hasher.hash("246810")).isNotEqualTo(hasher.hash("246811"));
    }

    @Test
    void the_hash_is_short_text_safe_to_store_in_the_database() {
        assertThat(hasher.hash("a-very-very-long-session-token"))
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
    }
}
