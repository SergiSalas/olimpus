package com.sergisalas.olimpus.matching.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Gente;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunDailyRoundTest {

    private static final LocalDate HOY = Gente.HOY;

    private MundoDeRondas mundo;
    private RunDailyRound ronda;

    @BeforeEach
    void setUp() {
        mundo = new MundoDeRondas();
        ronda = mundo.rondaDiaria();
    }

    @Test
    void la_ronda_crea_conversaciones_abiertas_que_cierran_a_las_diez_de_la_noche() {
        mundo.gente.addAll(Gente.poblacion(10, 1));

        var resultado = ronda.execute(HOY, RoundKind.PRINCIPAL);

        assertThat(resultado.created()).isNotEmpty();
        for (Conversation c : resultado.created()) {
            assertThat(c.state()).isEqualTo(ConversationState.ABIERTA);
            assertThat(c.isSilent()).isTrue();
            // 4:00 y 22:00 en Madrid son 02:00 y 20:00 en UTC (horario de verano).
            assertThat(c.opensAt()).isEqualTo(Instant.parse("2026-09-12T02:00:00Z"));
            assertThat(c.closesAt()).isEqualTo(Instant.parse("2026-09-12T20:00:00Z"));
        }
    }

    @Test
    void lanzar_la_misma_ronda_dos_veces_no_reparte_dos_veces() {
        mundo.gente.addAll(Gente.poblacion(10, 2));

        var primera = ronda.execute(HOY, RoundKind.PRINCIPAL);
        var segunda = ronda.execute(HOY, RoundKind.PRINCIPAL);

        assertThat(primera.alreadyRan()).isFalse();
        assertThat(segunda.alreadyRan()).isTrue();
        assertThat(segunda.created()).isEmpty();
        assertThat(mundo.guardadas).hasSize(primera.created().size());
    }

    @Test
    void nadie_tiene_dos_conversaciones_el_mismo_dia() {
        mundo.gente.addAll(Gente.poblacion(30, 3));

        ronda.execute(HOY, RoundKind.PRINCIPAL);
        var repesca = ronda.execute(HOY, RoundKind.REPESCA);

        // En la repesca, los que ya tienen conversacion viva no entran. Y como en
        // el paso 4 todavia no hay chat, todas estan en silencio y se cancelan,
        // asi que vuelven todos al reparto.
        for (Profile persona : mundo.gente) {
            long abiertas =
                    mundo.guardadas.values().stream()
                            .filter(c -> c.isOpen() && c.involves(persona.accountId()))
                            .count();
            assertThat(abiertas).as("%s tiene %d conversaciones abiertas", persona.nickname(), abiertas)
                    .isLessThanOrEqualTo(1);
        }
        assertThat(repesca.cancelled()).isNotEmpty();
    }

    @Test
    void la_repesca_cancela_solo_las_que_siguen_en_silencio() {
        mundo.gente.addAll(Gente.poblacion(20, 4));
        var principal = ronda.execute(HOY, RoundKind.PRINCIPAL);

        // Una de ellas arranca: escriben los dos.
        Conversation hablando = principal.created().get(0);
        mundo.conversations.save(
                new Conversation(
                        hablando.id(),
                        hablando.roundDate(),
                        hablando.roundKind(),
                        hablando.accountA(),
                        hablando.accountB(),
                        hablando.origin(),
                        hablando.score(),
                        hablando.opensAt(),
                        hablando.closesAt(),
                        ConversationState.ABIERTA,
                        3,
                        2));

        var repesca = ronda.execute(HOY, RoundKind.REPESCA);

        assertThat(repesca.cancelled()).noneMatch(c -> c.id().equals(hablando.id()));
        assertThat(mundo.guardadas.get(hablando.id()).state()).isEqualTo(ConversationState.ABIERTA);
    }

    @Test
    void quien_esta_hablando_no_entra_en_la_repesca() {
        mundo.gente.addAll(Gente.poblacion(20, 5));
        var principal = ronda.execute(HOY, RoundKind.PRINCIPAL);

        Conversation hablando = principal.created().get(0);
        mundo.conversations.save(
                new Conversation(
                        hablando.id(),
                        hablando.roundDate(),
                        hablando.roundKind(),
                        hablando.accountA(),
                        hablando.accountB(),
                        hablando.origin(),
                        hablando.score(),
                        hablando.opensAt(),
                        hablando.closesAt(),
                        ConversationState.ABIERTA,
                        1,
                        1));

        var repesca = ronda.execute(HOY, RoundKind.REPESCA);

        assertThat(repesca.created())
                .noneMatch(
                        c -> c.involves(hablando.accountA()) || c.involves(hablando.accountB()));
    }

    @Test
    void la_repesca_da_conversacion_a_quien_se_quedo_fuera() {
        // Dos mujeres que buscan hombres y un hombre: una se queda sin pareja.
        Profile ana = Gente.persona().nickname("Ana").gender(Gender.MUJER).busca(Gender.HOMBRE).build();
        Profile eva = Gente.persona().nickname("Eva").gender(Gender.MUJER).busca(Gender.HOMBRE).build();
        Profile leo = Gente.persona().nickname("Leo").gender(Gender.HOMBRE).busca(Gender.MUJER).build();
        mundo.gente.addAll(java.util.List.of(ana, eva, leo));

        var principal = ronda.execute(HOY, RoundKind.PRINCIPAL);
        assertThat(principal.created()).hasSize(1);
        assertThat(principal.leftOut()).hasSize(1);

        var repesca = ronda.execute(HOY, RoundKind.REPESCA);

        // La del principal se cancela por silencio, asi que en la repesca vuelven
        // a entrar los tres y la que se quedo fuera tiene otra oportunidad.
        assertThat(repesca.cancelled()).hasSize(1);
        assertThat(repesca.created()).hasSize(1);
    }

    @Test
    void sin_nadie_registrado_la_ronda_no_hace_nada() {
        var resultado = ronda.execute(HOY, RoundKind.PRINCIPAL);

        assertThat(resultado.created()).isEmpty();
        assertThat(resultado.peopleInPool()).isZero();
        assertThat(mundo.guardadas).isEmpty();
    }

    @Test
    void la_misma_ronda_repartida_dos_dias_distintos_da_parejas_distintas() {
        mundo.gente.addAll(Gente.poblacion(40, 6));

        var hoy = ronda.execute(HOY, RoundKind.PRINCIPAL);
        var manana = ronda.execute(HOY.plusDays(1), RoundKind.PRINCIPAL);

        assertThat(parejasDe(hoy)).isNotEqualTo(parejasDe(manana));
    }

    private static java.util.Set<String> parejasDe(RunDailyRound.RoundResult resultado) {
        return resultado.created().stream()
                .map(c -> c.accountA() + "+" + c.accountB())
                .collect(java.util.stream.Collectors.toSet());
    }
}
