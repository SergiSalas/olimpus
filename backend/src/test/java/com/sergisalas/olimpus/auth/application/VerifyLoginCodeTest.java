package com.sergisalas.olimpus.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.auth.FakeAuthWorld;
import com.sergisalas.olimpus.auth.domain.InvalidLoginCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifyLoginCodeTest {

    private FakeAuthWorld mundo;
    private RequestLoginCode pedirCodigo;
    private VerifyLoginCode meterCodigo;

    @BeforeEach
    void setUp() {
        mundo = new FakeAuthWorld();
        pedirCodigo =
                new RequestLoginCode(
                        mundo.codes, mundo.secrets, mundo.hasher, mundo.sender, mundo.clock);
        meterCodigo =
                new VerifyLoginCode(
                        mundo.codes,
                        mundo.accounts,
                        mundo.sessions,
                        mundo.secrets,
                        mundo.hasher,
                        mundo.clock);
    }

    @Test
    void con_el_codigo_bueno_nace_la_cuenta_y_empieza_la_sesion() {
        mundo.nextCode = "123456";
        pedirCodigo.execute("ana@example.com");

        VerifyLoginCode.StartedSession sesion = meterCodigo.execute("ana@example.com", "123456");

        assertThat(sesion.isNew()).isTrue();
        assertThat(sesion.token()).isEqualTo("llave-de-sesion");
        assertThat(sesion.expiresAt()).isEqualTo(mundo.now.plus(java.time.Duration.ofDays(90)));
        assertThat(mundo.cuentas).containsKey("ana@example.com");
        assertThat(mundo.sesiones).containsKey("huella(llave-de-sesion)");
    }

    @Test
    void la_segunda_vez_es_la_misma_cuenta() {
        pedirCodigo.execute("ana@example.com");
        var primera = meterCodigo.execute("ana@example.com", mundo.nextCode);

        pedirCodigo.execute("ana@example.com");
        mundo.nextToken = "otra-llave";
        var segunda = meterCodigo.execute("ana@example.com", mundo.nextCode);

        assertThat(segunda.isNew()).isFalse();
        assertThat(segunda.account().id()).isEqualTo(primera.account().id());
        assertThat(mundo.sesiones).hasSize(2);
    }

    @Test
    void el_codigo_se_gasta_al_usarlo() {
        pedirCodigo.execute("ana@example.com");
        meterCodigo.execute("ana@example.com", mundo.nextCode);

        assertThatThrownBy(() -> meterCodigo.execute("ana@example.com", mundo.nextCode))
                .isInstanceOf(InvalidLoginCodeException.class);
    }

    @Test
    void a_los_diez_minutos_y_un_segundo_ya_no_vale() {
        pedirCodigo.execute("ana@example.com");
        mundo.now = mundo.now.plusSeconds(601);

        assertThatThrownBy(() -> meterCodigo.execute("ana@example.com", mundo.nextCode))
                .isInstanceOf(InvalidLoginCodeException.class);
        assertThat(mundo.codigos).isEmpty();
    }

    @Test
    void cada_fallo_gasta_un_intento_y_al_quinto_desaparece_el_codigo() {
        mundo.nextCode = "123456";
        pedirCodigo.execute("ana@example.com");

        for (int intento = 1; intento <= 4; intento++) {
            assertThatThrownBy(() -> meterCodigo.execute("ana@example.com", "000000"))
                    .isInstanceOf(InvalidLoginCodeException.class);
            assertThat(mundo.codigos.get("ana@example.com").attemptsLeft()).isEqualTo(5 - intento);
        }

        assertThatThrownBy(() -> meterCodigo.execute("ana@example.com", "000000"))
                .isInstanceOf(InvalidLoginCodeException.class);
        assertThat(mundo.codigos).isEmpty();

        // Y el codigo bueno tampoco sirve ya: hay que pedir otro.
        assertThatThrownBy(() -> meterCodigo.execute("ana@example.com", "123456"))
                .isInstanceOf(InvalidLoginCodeException.class);
    }

    @Test
    void el_codigo_de_otro_email_no_sirve() {
        mundo.nextCode = "123456";
        pedirCodigo.execute("ana@example.com");

        assertThatThrownBy(() -> meterCodigo.execute("carlos@example.com", "123456"))
                .isInstanceOf(InvalidLoginCodeException.class);
    }

    @Test
    void los_espacios_de_mas_al_escribir_el_codigo_se_perdonan() {
        mundo.nextCode = "123456";
        pedirCodigo.execute("ana@example.com");

        assertThat(meterCodigo.execute("ana@example.com", "  123456 ").account().email().value())
                .isEqualTo("ana@example.com");
    }
}
