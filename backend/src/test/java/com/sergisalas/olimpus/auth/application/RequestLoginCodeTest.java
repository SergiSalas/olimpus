package com.sergisalas.olimpus.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.auth.FakeAuthWorld;
import com.sergisalas.olimpus.auth.domain.InvalidEmailException;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestLoginCodeTest {

    private FakeAuthWorld mundo;
    private RequestLoginCode pedirCodigo;

    @BeforeEach
    void setUp() {
        mundo = new FakeAuthWorld();
        pedirCodigo =
                new RequestLoginCode(
                        mundo.codes, mundo.secrets, mundo.hasher, mundo.sender, mundo.clock);
    }

    @Test
    void manda_el_codigo_al_email_y_guarda_solo_su_huella() {
        mundo.nextCode = "246810";

        pedirCodigo.execute("Ana@Example.com");

        assertThat(mundo.enviados).containsExactly("ana@example.com:246810");
        // Se guarda la huella, no el codigo. Que la huella de verdad sea
        // irreversible se comprueba en Sha256HasherTest.
        LoginCode guardado = mundo.codigos.get("ana@example.com");
        assertThat(guardado.codeHash()).isEqualTo(mundo.hasher.hash("246810"));
    }

    @Test
    void el_codigo_caduca_a_los_diez_minutos_y_trae_cinco_intentos() {
        pedirCodigo.execute("ana@example.com");

        LoginCode guardado = mundo.codigos.get("ana@example.com");
        assertThat(guardado.expiresAt()).isEqualTo(mundo.now.plusSeconds(600));
        assertThat(guardado.attemptsLeft()).isEqualTo(5);
    }

    @Test
    void pedir_otro_codigo_sustituye_al_anterior() {
        mundo.nextCode = "111111";
        pedirCodigo.execute("ana@example.com");
        mundo.nextCode = "222222";
        pedirCodigo.execute("ana@example.com");

        assertThat(mundo.codigos).hasSize(1);
        assertThat(mundo.codigos.get("ana@example.com").codeHash()).isEqualTo("huella(222222)");
    }

    @Test
    void pedir_codigo_no_crea_ninguna_cuenta() {
        pedirCodigo.execute("ana@example.com");

        assertThat(mundo.cuentas).isEmpty();
    }

    @Test
    void un_email_con_mala_pinta_no_manda_nada() {
        assertThatThrownBy(() -> pedirCodigo.execute("ana(arroba)example.com"))
                .isInstanceOf(InvalidEmailException.class);

        assertThat(mundo.enviados).isEmpty();
    }
}
