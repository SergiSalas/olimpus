package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.TestPeople.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScorerTest {

    private final MatchContext ctx = MatchContext.on(TODAY).build();

    @Test
    void a_pair_is_worth_what_it_is_worth_for_whoever_comes_off_worse() {
        // Carlos suits Ana, but Ana is outside Carlos's range.
        Profile ana = TestPeople.person().age(30).ageRange(18, 99).build();
        Profile carlos = TestPeople.person().age(30).ageRange(18, 22).build();

        ScoredPair pair = Scorer.score(ana, carlos, ctx);

        assertThat(pair.score()).isEqualTo(Math.min(pair.sideA(), pair.sideB()));
        assertThat(pair.sideA()).isGreaterThan(pair.sideB());
    }

    @Test
    void the_score_is_always_between_zero_and_one() {
        for (Profile a : TestPeople.population(20, 7)) {
            for (Profile b : TestPeople.population(20, 8)) {
                assertThat(Scorer.directional(a, b, ctx)).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void close_age_is_the_heaviest_factor() {
        Profile ana = TestPeople.person().age(30).build();
        Profile sameAge = TestPeople.person().age(31).build();
        Profile twentyYearsOlder = TestPeople.person().age(51).build();

        assertThat(Scorer.directional(ana, sameAge, ctx))
                .isGreaterThan(Scorer.directional(ana, twentyYearsOlder, ctx));

        assertThat(Scorer.ageClosenessFit(ana, sameAge, ctx)).isGreaterThan(0.8);
        assertThat(Scorer.ageClosenessFit(ana, twentyYearsOlder, ctx)).isLessThan(0.05);
    }

    @Test
    void sharing_a_rare_interest_is_worth_much_more_than_sharing_a_common_one() {
        // A population where everyone picks "travel" and almost nobody "kendo".
        List<Profile> population = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            population.add(TestPeople.person().interests("travel", "movies", "reading", "running", "surfing").build());
        }
        population.add(TestPeople.person().interests("kendo", "movies", "reading", "running", "surfing").build());

        MatchContext withWeights =
                MatchContext.on(TODAY)
                        .interestWeights(InterestWeights.fromPopulation(population))
                        .build();

        Profile ana = TestPeople.person().interests("travel", "kendo", "art", "theatre", "wine").build();
        Profile sharesCommon =
                TestPeople.person().interests("travel", "podcasts", "dancing", "surfing", "cycling").build();
        Profile sharesRare =
                TestPeople.person().interests("kendo", "podcasts", "dancing", "surfing", "cycling").build();

        double common = withWeights.interestWeights().similarity(ana.interests(), sharesCommon.interests());
        double rare = withWeights.interestWeights().similarity(ana.interests(), sharesRare.interests());

        assertThat(rare).isGreaterThan(common * 2);
    }

    @Test
    void two_quiet_people_promise_less_than_two_sociable_ones() {
        Profile quietA = TestPeople.person().sociability(1).conversationDepth(3).build();
        Profile quietB = TestPeople.person().sociability(1).conversationDepth(3).build();
        Profile sociableA = TestPeople.person().sociability(5).conversationDepth(3).build();
        Profile sociableB = TestPeople.person().sociability(5).conversationDepth(3).build();

        assertThat(Scorer.sociabilityFit(sociableA, sociableB))
                .isGreaterThan(Scorer.sociabilityFit(quietA, quietB));
    }

    @Test
    void wanting_the_same_kind_of_conversation_adds_up() {
        Profile deepA = TestPeople.person().sociability(3).conversationDepth(5).build();
        Profile deepB = TestPeople.person().sociability(3).conversationDepth(5).build();
        Profile light = TestPeople.person().sociability(3).conversationDepth(1).build();

        assertThat(Scorer.sociabilityFit(deepA, deepB))
                .isGreaterThan(Scorer.sociabilityFit(deepA, light));
    }

    @Test
    void someone_you_have_never_talked_to_is_full_novelty() {
        Profile ana = TestPeople.anyone();
        Profile stranger = TestPeople.anyone();

        assertThat(Scorer.noveltyFit(ana, stranger, ctx)).isEqualTo(1.0);
    }

    @Test
    void repeating_with_yesterdays_person_scores_almost_zero_and_recovers_with_the_days() {
        Profile ana = TestPeople.anyone();
        Profile carlos = TestPeople.anyone();

        MatchContext yesterday =
                MatchContext.on(TODAY).talked(ana.accountId(), carlos.accountId(), TODAY.minusDays(1)).build();
        MatchContext aMonthAgo =
                MatchContext.on(TODAY).talked(ana.accountId(), carlos.accountId(), TODAY.minusDays(30)).build();

        assertThat(Scorer.noveltyFit(ana, carlos, yesterday)).isLessThan(0.15);
        assertThat(Scorer.noveltyFit(ana, carlos, aMonthAgo)).isGreaterThan(0.9);
    }

    @Test
    void the_weights_add_up_to_one() {
        assertThat(Scorer.WEIGHT_SUM).isEqualTo(1.0);
    }

    @Test
    void the_intent_table_is_symmetric() {
        for (var a : Intent.values()) {
            for (var b : Intent.values()) {
                assertThat(IntentFit.between(a, b)).isEqualTo(IntentFit.between(b, a));
            }
        }
    }
}
