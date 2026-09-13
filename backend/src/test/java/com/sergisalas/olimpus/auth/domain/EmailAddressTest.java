package com.sergisalas.olimpus.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

    @Test
    void trims_and_lower_cases() {
        assertThat(EmailAddress.of("  Ana.Perez@Example.COM  ").value())
                .isEqualTo("ana.perez@example.com");
    }

    @Test
    void two_ways_of_writing_the_same_email_are_the_same_email() {
        assertThat(EmailAddress.of("ANA@x.com")).isEqualTo(EmailAddress.of("ana@x.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ana", "ana@", "@x.com", "ana@x", "ana perez@x.com"})
    void rejects_what_is_not_an_email(String garbage) {
        assertThatThrownBy(() -> EmailAddress.of(garbage)).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rejects_a_missing_email() {
        assertThatThrownBy(() -> EmailAddress.of(null))
                .isInstanceOf(InvalidEmailException.class)
                .extracting("messageKey")
                .isEqualTo("email.missing");
    }
}
