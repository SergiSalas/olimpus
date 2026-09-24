package com.sergisalas.olimpus.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.Origin;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InsightsTest {

    private static Insights.ByOrigin row(
            Origin origin, int pairs, int bothWrote, int connections, int still) {
        return new Insights.ByOrigin(origin, pairs, bothWrote, connections, still);
    }

    private static Insights withRows(List<Insights.ByOrigin> rows) {
        return new Insights(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 15),
                rows,
                new Insights.Alarms(0, 0, 0, 0, 0, 0, 0));
    }

    @Nested
    class Rates {

        @Test
        void a_kind_of_pair_nobody_got_does_not_divide_by_zero() {
            Insights.ByOrigin empty = row(Origin.RANDOM, 0, 0, 0, 0);

            assertThat(empty.tookOffRate()).isZero();
            assertThat(empty.connectionRate()).isZero();
            assertThat(empty.survivalRate()).isZero();
        }

        @Test
        void the_rates_are_over_the_right_denominator() {
            // 100 handed out, 60 took off, 20 connected, 5 alive two days later.
            Insights.ByOrigin best = row(Origin.BEST_MATCH, 100, 60, 20, 5);

            assertThat(best.tookOffRate()).isEqualTo(0.60);
            assertThat(best.connectionRate()).isEqualTo(0.20);
            // Survival is over the connections, not over the pairs: 5 of 20.
            assertThat(best.survivalRate()).isEqualTo(0.25);
        }
    }

    @Nested
    class AgainstRandom {

        @Test
        void says_how_much_better_the_chosen_pairs_do() {
            Insights insights =
                    withRows(
                            List.of(
                                    row(Origin.BEST_MATCH, 100, 70, 20, 8),
                                    row(Origin.RANDOM, 100, 50, 10, 3)));

            assertThat(insights.liftOverRandom()).isCloseTo(2.0, Offset.offset(0.001));
        }

        @Test
        void can_also_say_that_the_algorithm_is_doing_worse() {
            Insights insights =
                    withRows(
                            List.of(
                                    row(Origin.BEST_MATCH, 100, 70, 8, 2),
                                    row(Origin.RANDOM, 100, 50, 16, 5)));

            assertThat(insights.liftOverRandom()).isLessThan(1);
        }

        @Test
        void without_random_pairs_there_is_nothing_to_compare_against() {
            Insights insights = withRows(List.of(row(Origin.BEST_MATCH, 100, 70, 20, 8)));

            assertThat(insights.liftOverRandom()).isZero();
        }
    }

    @Nested
    class HowEvenlyItSpreads {

        @Test
        void perfectly_even_means_the_top_tenth_takes_a_tenth() {
            List<Integer> everyoneTheSame = java.util.Collections.nCopies(100, 5);

            assertThat(Concentration.shareTakenByTopTenth(everyoneTheSame))
                    .isCloseTo(0.10, Offset.offset(0.001));
            assertThat(Concentration.evenShare(100)).isCloseTo(0.10, Offset.offset(0.001));
        }

        @Test
        void a_few_taking_everything_shows_up_as_a_big_share() {
            List<Integer> lopsided = new java.util.ArrayList<>(java.util.Collections.nCopies(90, 0));
            lopsided.addAll(java.util.Collections.nCopies(10, 50));

            assertThat(Concentration.shareTakenByTopTenth(lopsided)).isEqualTo(1.0);
        }

        @Test
        void nobody_talking_to_anybody_is_not_a_division_by_zero() {
            assertThat(Concentration.shareTakenByTopTenth(List.of())).isZero();
            assertThat(Concentration.shareTakenByTopTenth(List.of(0, 0, 0))).isZero();
            assertThat(Concentration.evenShare(0)).isZero();
        }

        @Test
        void with_very_few_people_it_still_gives_a_readable_number() {
            assertThat(Concentration.shareTakenByTopTenth(List.of(3, 1, 1, 1))).isCloseTo(0.5, Offset.offset(0.001));
        }
    }
}
