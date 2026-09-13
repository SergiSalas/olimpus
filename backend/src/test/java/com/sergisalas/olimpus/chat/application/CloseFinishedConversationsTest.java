package com.sergisalas.olimpus.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.application.MundoDeRondas;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Gente;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloseFinishedConversationsTest {

    private MundoDeRondas mundo;
    private CloseFinishedConversations cerrar;

    @BeforeEach
    void setUp() {
        mundo = new MundoDeRondas();
        mundo.gente.addAll(Gente.poblacion(10, 1));
        mundo.rondaDiaria().execute(Gente.HOY, RoundKind.PRINCIPAL);
        cerrar = new CloseFinishedConversations(mundo.conversations, mundo.schedule, mundo.clock);
    }

    @Test
    void antes_de_las_diez_no_se_cierra_nada() {
        mundo.ahora = Instant.parse("2026-09-12T19:59:00Z"); // 21:59 en Madrid

        assertThat(cerrar.execute()).isEmpty();
        assertThat(mundo.guardadas.values()).allMatch(c -> c.state() == ConversationState.ABIERTA);
    }

    @Test
    void a_las_diez_en_punto_se_cierran_todas() {
        mundo.ahora = Instant.parse("2026-09-12T20:00:00Z"); // 22:00 en Madrid

        var cerradas = cerrar.execute();

        assertThat(cerradas).isNotEmpty();
        assertThat(mundo.guardadas.values()).allMatch(c -> c.state() == ConversationState.CERRADA);
    }

    @Test
    void si_el_servidor_estuvo_caido_las_de_ayer_tambien_se_cierran() {
        // Se despierta a las 10 de la mañana del dia siguiente.
        mundo.ahora = Instant.parse("2026-09-13T08:00:00Z");

        assertThat(cerrar.execute()).isNotEmpty();
        assertThat(mundo.guardadas.values()).noneMatch(c -> c.state() == ConversationState.ABIERTA);
    }

    @Test
    void cerrar_dos_veces_no_vuelve_a_cerrar_lo_ya_cerrado() {
        mundo.ahora = Instant.parse("2026-09-12T20:00:00Z");
        cerrar.execute();

        assertThat(cerrar.execute()).isEmpty();
    }
}
