package com.sergisalas.olimpus.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

    @Test
    void quita_espacios_y_pasa_a_minusculas() {
        assertThat(EmailAddress.of("  Ana.Perez@Example.COM  ").value())
                .isEqualTo("ana.perez@example.com");
    }

    @Test
    void dos_formas_de_escribir_el_mismo_email_son_el_mismo_email() {
        assertThat(EmailAddress.of("ANA@x.com")).isEqualTo(EmailAddress.of("ana@x.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ana", "ana@", "@x.com", "ana@x", "ana perez@x.com"})
    void rechaza_lo_que_no_es_un_email(String basura) {
        assertThatThrownBy(() -> EmailAddress.of(basura)).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void rechaza_el_email_vacio() {
        assertThatThrownBy(() -> EmailAddress.of(null)).isInstanceOf(InvalidEmailException.class);
    }
}
