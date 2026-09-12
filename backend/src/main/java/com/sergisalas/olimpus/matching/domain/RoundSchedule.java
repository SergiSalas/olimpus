package com.sergisalas.olimpus.matching.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Las horas del dia en Olimpus.
 *
 * <p>El reparto sale a las 4:00 y la conversacion cierra a las 22:00 del mismo
 * dia: las dieciocho horas del documento. La repesca de mediodia comparte la
 * misma hora de cierre, para que todo el mundo vuelva a la app a la vez.
 *
 * <p>Las horas se guardan siempre como instante en UTC, pero se calculan en la
 * zona de la comunidad: si la app crece a otro pais, esa zona cambia sin tocar
 * nada mas.
 */
public record RoundSchedule(ZoneId zone, LocalTime principal, LocalTime repesca, LocalTime cierre) {

    public static final LocalTime HORA_PRINCIPAL = LocalTime.of(4, 0);
    public static final LocalTime HORA_REPESCA = LocalTime.of(14, 0);
    public static final LocalTime HORA_CIERRE = LocalTime.of(22, 0);

    public static RoundSchedule of(ZoneId zone) {
        return new RoundSchedule(zone, HORA_PRINCIPAL, HORA_REPESCA, HORA_CIERRE);
    }

    public Instant opensAt(LocalDate date, RoundKind kind) {
        LocalTime hora = kind == RoundKind.PRINCIPAL ? principal : repesca;
        return date.atTime(hora).atZone(zone).toInstant();
    }

    public Instant closesAt(LocalDate date) {
        return date.atTime(cierre).atZone(zone).toInstant();
    }

    /** Que dia de reparto es un instante dado, en la zona de la comunidad. */
    public LocalDate dateOf(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }
}
