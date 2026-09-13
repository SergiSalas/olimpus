package com.sergisalas.olimpus.matching.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * The hours of the day in Olimpus.
 *
 * <p>The round comes out at 4:00 and the conversation closes at 22:00 the same
 * day: the eighteen hours of the product document. The midday second-chance
 * round shares the same closing time, so everyone comes back to the app at once.
 *
 * <p>Times are always stored as UTC instants, but computed in the community's
 * zone: if the app grows into another country, that zone changes and nothing
 * else does.
 */
public record RoundSchedule(ZoneId zone, LocalTime main, LocalTime secondChance, LocalTime closing) {

    public static final LocalTime MAIN_TIME = LocalTime.of(4, 0);
    public static final LocalTime SECOND_CHANCE_TIME = LocalTime.of(14, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);

    public static RoundSchedule of(ZoneId zone) {
        return new RoundSchedule(zone, MAIN_TIME, SECOND_CHANCE_TIME, CLOSING_TIME);
    }

    public Instant opensAt(LocalDate date, RoundKind kind) {
        LocalTime time = kind == RoundKind.MAIN ? main : secondChance;
        return date.atTime(time).atZone(zone).toInstant();
    }

    public Instant closesAt(LocalDate date) {
        return date.atTime(closing).atZone(zone).toInstant();
    }

    /** Which round day an instant belongs to, in the community's zone. */
    public LocalDate dateOf(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }
}
