package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.TestPeople.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DailyRoundTest {

    private final MatchContext ctx = MatchContext.on(TODAY).build();

    @Test
    void with_a_single_person_there_is_no_round() {
        assertThat(DailyRound.plan(List.of(TestPeople.anyone()), ctx, new Random(1))).isEmpty();
        assertThat(DailyRound.plan(List.of(), ctx, new Random(1))).isEmpty();
    }

    @Test
    void two_compatible_people_end_up_together() {
        Profile ana = TestPeople.person().nickname("Ana").build();
        Profile carlos = TestPeople.person().nickname("Carlos").build();

        List<Match> round = DailyRound.plan(List.of(ana, carlos), ctx, new Random(1));

        assertThat(round).hasSize(1);
        assertThat(round.get(0).involves(ana.accountId())).isTrue();
        assertThat(round.get(0).involves(carlos.accountId())).isTrue();
    }

    @Test
    void nobody_appears_in_two_pairs_on_the_same_day() {
        List<Profile> people = TestPeople.population(60, 42);

        List<Match> round = DailyRound.plan(people, ctx, new Random(42));

        Set<UUID> seen = new HashSet<>();
        for (Match match : round) {
            assertThat(seen.add(match.accountA())).as("A repeated").isTrue();
            assertThat(seen.add(match.accountB())).as("B repeated").isTrue();
        }
    }

    @Test
    void every_pair_in_the_round_passes_the_hard_filters_random_ones_included() {
        List<Profile> people = TestPeople.population(80, 7);
        MatchContext withBlocks =
                MatchContext.on(TODAY)
                        .blocked(people.get(0).accountId(), people.get(1).accountId())
                        .blocked(people.get(2).accountId(), people.get(3).accountId())
                        .build();

        List<Match> round = DailyRound.plan(people, withBlocks, new Random(7));

        assertThat(round).isNotEmpty();
        for (Match match : round) {
            Profile a = find(people, match.accountA());
            Profile b = find(people, match.accountB());
            assertThat(Filters.passesHard(a, b, withBlocks))
                    .as("pair with origin %s", match.origin())
                    .isTrue();
        }
    }

    @Test
    void people_who_blocked_each_other_never_appear_together() {
        List<Profile> people = TestPeople.population(40, 3);
        UUID one = people.get(0).accountId();
        UUID other = people.get(1).accountId();
        MatchContext withBlock = MatchContext.on(TODAY).blocked(one, other).build();

        List<Match> round = DailyRound.plan(people, withBlock, new Random(3));

        assertThat(round)
                .noneMatch(match -> match.involves(one) && match.involves(other));
    }

    @Test
    void part_of_the_round_is_random_and_part_is_discovery() {
        List<Profile> people = TestPeople.population(100, 11);

        List<Match> round = DailyRound.plan(people, ctx, new Random(11));

        long random = round.stream().filter(m -> m.origin() == Origin.RANDOM).count();
        long discovery = round.stream().filter(m -> m.origin() == Origin.DISCOVERY).count();
        long best = round.stream().filter(m -> m.origin() == Origin.BEST_MATCH).count();

        // 100 people make about 50 pairs, and we want 10% of them to be random
        // and another 10% discovery: about 5 of each.
        long total = round.size();
        assertThat(random).isBetween(3L, 8L);
        assertThat(discovery).isBetween(3L, 8L);
        assertThat(random / (double) total).isBetween(0.05, 0.16);
        assertThat(discovery / (double) total).isBetween(0.05, 0.16);
        assertThat(best).isGreaterThan(random + discovery);
    }

    @Test
    void the_round_is_repeatable_with_the_same_seed() {
        List<Profile> people = TestPeople.population(50, 5);

        List<Match> first = DailyRound.plan(people, ctx, new Random(99));
        List<Match> second = DailyRound.plan(people, ctx, new Random(99));
        List<Match> otherSeed = DailyRound.plan(people, ctx, new Random(100));

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(otherSeed);
    }

    @Test
    void chosen_pairs_look_more_promising_than_random_ones() {
        List<Profile> people = TestPeople.population(200, 21);
        MatchContext withWeights =
                MatchContext.on(TODAY).interestWeights(InterestWeights.fromPopulation(people)).build();

        List<Match> round = DailyRound.plan(people, withWeights, new Random(21));

        double chosenAverage = average(round, Origin.BEST_MATCH);
        double randomAverage = average(round, Origin.RANDOM);

        // If this failed, the round would not be using the score at all.
        assertThat(chosenAverage).isGreaterThan(randomAverage);
    }

    @Test
    void discovery_is_worse_than_best_match_but_better_than_random() {
        List<Profile> people = TestPeople.population(200, 33);
        MatchContext withWeights =
                MatchContext.on(TODAY).interestWeights(InterestWeights.fromPopulation(people)).build();

        List<Match> round = DailyRound.plan(people, withWeights, new Random(33));

        // If discovery gave the same as random, two parts of the round would be
        // measuring the same thing and one of them would be useless.
        assertThat(average(round, Origin.DISCOVERY)).isLessThan(average(round, Origin.BEST_MATCH));
        assertThat(average(round, Origin.DISCOVERY)).isGreaterThan(average(round, Origin.RANDOM));
    }

    @Test
    void whoever_fits_nobody_is_left_out_and_appears_in_the_second_chance_list() {
        Profile ana = TestPeople.person().gender(Gender.WOMAN).seeking(Gender.MAN).build();
        Profile eva = TestPeople.person().gender(Gender.WOMAN).seeking(Gender.MAN).build();
        Profile carlos = TestPeople.person().gender(Gender.MAN).seeking(Gender.WOMAN).build();

        List<Profile> people = List.of(ana, eva, carlos);
        List<Match> round = DailyRound.plan(people, ctx, new Random(1));

        assertThat(round).hasSize(1);
        assertThat(DailyRound.leftOut(people, round)).hasSize(1);
    }

    @Test
    void with_many_compatible_people_almost_nobody_is_left_without_a_conversation() {
        List<Profile> people = TestPeople.population(100, 77);

        List<Match> round = DailyRound.plan(people, ctx, new Random(77));

        assertThat(DailyRound.leftOut(people, round).size()).isLessThan(15);
    }

    @Test
    void a_round_of_four_hundred_people_is_matched_in_under_a_second() {
        List<Profile> people = TestPeople.population(400, 4);
        MatchContext withWeights =
                MatchContext.on(TODAY).interestWeights(InterestWeights.fromPopulation(people)).build();

        long before = System.currentTimeMillis();
        List<Match> round = DailyRound.plan(people, withWeights, new Random(4));
        long took = System.currentTimeMillis() - before;

        assertThat(round).isNotEmpty();
        assertThat(took).as("took %d ms", took).isLessThan(1000);
    }

    private static double average(List<Match> round, Origin origin) {
        List<Double> values = new ArrayList<>();
        for (Match match : round) {
            if (match.origin() == origin) values.add(match.score());
        }
        assertThat(values).as("no pairs of kind %s", origin).isNotEmpty();
        return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static Profile find(List<Profile> people, UUID id) {
        return people.stream()
                .filter(p -> p.accountId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("id not in the pool"));
    }
}
