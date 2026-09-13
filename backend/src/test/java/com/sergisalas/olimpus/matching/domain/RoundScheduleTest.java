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
    void the_round_comes_out_at_four_and_closes_at_ten_at_night() {
        LocalDate day = LocalDate.of(2026, 9, 12);

        // In September Madrid is two hours ahead of UTC.
        assertThat(madrid.opensAt(day, RoundKind.MAIN))
                .isEqualTo(Instant.parse("2026-09-12T02:00:00Z"));
        assertThat(madrid.closesAt(day)).isEqualTo(Instant.parse("2026-09-12T20:00:00Z"));
    }

    @Test
    void the_main_conversation_lasts_eighteen_hours() {
        LocalDate day = LocalDate.of(2026, 9, 12);

        Duration lasts =
                Duration.between(madrid.opensAt(day, RoundKind.MAIN), madrid.closesAt(day));

        assertThat(lasts).isEqualTo(Duration.ofHours(18));
    }

    @Test
    void the_second_chance_shares_the_closing_time() {
        LocalDate day = LocalDate.of(2026, 9, 12);

        assertThat(madrid.opensAt(day, RoundKind.SECOND_CHANCE))
                .isEqualTo(Instant.parse("2026-09-12T12:00:00Z"));
        assertThat(Duration.between(madrid.opensAt(day, RoundKind.SECOND_CHANCE), madrid.closesAt(day)))
                .isEqualTo(Duration.ofHours(8));
    }

    @Test
    void in_winter_the_times_are_still_peoples_times_not_utc() {
        LocalDate january = LocalDate.of(2027, 1, 15);

        // Without summer time Madrid is one hour ahead: people's 22:00 is
        // 21:00 UTC. The local time does not move, which is what matters for
        // everyone coming back to the app at the same time.
        assertThat(madrid.closesAt(january)).isEqualTo(Instant.parse("2027-01-15T21:00:00Z"));
    }

    @Test
    void the_round_day_is_computed_in_the_community_zone() {
        // 23:30 UTC on the 11th is already the early hours of the 12th in Madrid.
        assertThat(madrid.dateOf(Instant.parse("2026-09-11T23:30:00Z")))
                .isEqualTo(LocalDate.of(2026, 9, 12));
    }
}
