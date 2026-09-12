package com.sergisalas.olimpus.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RoundScheduleTest {

    private final RoundSchedule madrid = RoundSchedule.of(ZoneId.of("Europe/Madrid"));

    @Test
    void el_reparto_sale_a_las_cuatro_y_cierra_a_las_diez_de_la_noche() {
        LocalDate dia = LocalDate.of(2026, 9, 12);

        // En septiembre Madrid va dos horas por delante de UTC.
        assertThat(madrid.opensAt(dia, RoundKind.PRINCIPAL))
                .isEqualTo(Instant.parse("2026-09-12T02:00:00Z"));
        assertThat(madrid.closesAt(dia)).isEqualTo(Instant.parse("2026-09-12T20:00:00Z"));
    }

    @Test
    void la_conversacion_principal_dura_dieciocho_horas() {
        LocalDate dia = LocalDate.of(2026, 9, 12);

        Duration dura =
                Duration.between(madrid.opensAt(dia, RoundKind.PRINCIPAL), madrid.closesAt(dia));

        assertThat(dura).isEqualTo(Duration.ofHours(18));
    }

    @Test
    void la_repesca_comparte_la_hora_de_cierre() {
        LocalDate dia = LocalDate.of(2026, 9, 12);

        assertThat(madrid.opensAt(dia, RoundKind.REPESCA))
                .isEqualTo(Instant.parse("2026-09-12T12:00:00Z"));
        assertThat(Duration.between(madrid.opensAt(dia, RoundKind.REPESCA), madrid.closesAt(dia)))
                .isEqualTo(Duration.ofHours(8));
    }

    @Test
    void en_invierno_las_horas_siguen_siendo_las_de_la_gente_no_las_de_utc() {
        LocalDate enero = LocalDate.of(2027, 1, 15);

        // Sin horario de verano, Madrid va una hora por delante: las 22:00 de la
        // gente son las 21:00 UTC. La hora local no se mueve, que es lo que
        // importa para que todos vuelvan a la app a la misma hora.
        assertThat(madrid.closesAt(enero)).isEqualTo(Instant.parse("2027-01-15T21:00:00Z"));
    }

    @Test
    void el_dia_de_reparto_se_calcula_en_la_zona_de_la_comunidad() {
        // 23:30 UTC del dia 11 ya es la madrugada del 12 en Madrid.
        assertThat(madrid.dateOf(Instant.parse("2026-09-11T23:30:00Z")))
                .isEqualTo(LocalDate.of(2026, 9, 12));
    }
}
