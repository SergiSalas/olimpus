package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.TestPeople.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.LanguageSkill;
import com.sergisalas.olimpus.profile.domain.Profile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FiltersTest {

    private final MatchContext ctx = MatchContext.on(TODAY).build();

    @Nested
    class TheOnesThatNeverGiveWay {

        @Test
        void nobody_talks_to_themselves() {
            Profile ana = TestPeople.anyone();

            assertThat(Filters.passesHard(ana, ana, ctx)).isFalse();
        }

        @Test
        void gender_has_to_fit_on_both_sides() {
            Profile ana = TestPeople.person().gender(Gender.WOMAN).seeking(Gender.MAN).build();
            Profile carlos = TestPeople.person().gender(Gender.MAN).seeking(Gender.WOMAN).build();
            Profile luis = TestPeople.person().gender(Gender.MAN).seeking(Gender.MAN).build();

            assertThat(Filters.passesHard(ana, carlos, ctx)).isTrue();
            // Luis fits Ana, but Luis is not looking for women: no pair.
            assertThat(Filters.passesHard(ana, luis, ctx)).isFalse();
            assertThat(Filters.passesHard(luis, ana, ctx)).isFalse();
        }

        @Test
        void a_block_cuts_both_ways() {
            Profile ana = TestPeople.anyone();
            Profile carlos = TestPeople.anyone();
            MatchContext withBlock =
                    MatchContext.on(TODAY).blocked(ana.accountId(), carlos.accountId()).build();

            assertThat(Filters.passesHard(ana, carlos, withBlock)).isFalse();
            assertThat(Filters.passesHard(carlos, ana, withBlock)).isFalse();
        }

        @Test
        void however_long_the_wait_the_hard_core_does_not_move() {
            Profile ana = TestPeople.person().gender(Gender.WOMAN).seeking(Gender.MAN).build();
            Profile luis = TestPeople.person().gender(Gender.MAN).seeking(Gender.MAN).build();
            MatchContext waitingForAges =
                    MatchContext.on(TODAY).waiting(ana.accountId(), 500).build();

            assertThat(Filters.passesHard(ana, luis, waitingForAges)).isFalse();
        }
    }

    @Nested
    class TheOnesThatGiveWayWithWaiting {

        @Test
        void with_no_wait_preferences_are_respected_as_they_are() {
            Profile ana = TestPeople.person().maxDistanceKm(5).at(41.3874, 2.1686).build();
            Profile farAway = TestPeople.person().maxDistanceKm(50).at(41.4500, 2.1686).build();

            assertThat(Filters.passesSoft(ana, farAway, ctx, 0)).isFalse();
        }

        @Test
        void at_maximum_relaxation_the_radius_reaches_three_times() {
            Profile ana = TestPeople.person().maxDistanceKm(5).at(41.3874, 2.1686).build();
            // About 7 km north: outside 5 km, inside 15.
            Profile sevenKmAway = TestPeople.person().maxDistanceKm(50).at(41.4500, 2.1686).build();

            assertThat(Filters.passesSoft(ana, sevenKmAway, ctx, 1)).isTrue();
        }

        @Test
        void distance_is_mutual_too() {
            Profile ana = TestPeople.person().maxDistanceKm(50).at(41.3874, 2.1686).build();
            Profile homebody = TestPeople.person().maxDistanceKm(5).at(41.4500, 2.1686).build();

            assertThat(Filters.passesSoft(ana, homebody, ctx, 0)).isFalse();
        }

        @Test
        void the_age_range_widens_up_to_five_years_on_each_side() {
            Profile ana = TestPeople.person().age(30).ageRange(28, 35).build();
            Profile younger = TestPeople.person().age(25).ageRange(18, 99).build();

            assertThat(Filters.passesSoft(ana, younger, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(ana, younger, ctx, 1)).isTrue();
        }

        @Test
        void without_a_shared_language_there_is_no_conversation_even_after_a_long_wait() {
            Profile ana = TestPeople.person().languages(new LanguageSkill("es", LanguageSkill.Level.NATIVE)).build();
            Profile jan = TestPeople.person().languages(new LanguageSkill("de", LanguageSkill.Level.NATIVE)).build();

            assertThat(Filters.passesSoft(ana, jan, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(ana, jan, ctx, 1)).isFalse();
        }

        @Test
        void a_weak_shared_language_only_counts_after_waiting() {
            Profile ana =
                    TestPeople.person()
                            .languages(
                                    new LanguageSkill("es", LanguageSkill.Level.NATIVE),
                                    new LanguageSkill("en", LanguageSkill.Level.BASIC))
                            .build();
            Profile john =
                    TestPeople.person().languages(new LanguageSkill("en", LanguageSkill.Level.NATIVE)).build();

            assertThat(Filters.passesSoft(ana, john, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(ana, john, ctx, 1)).isTrue();
        }

        @Test
        void very_different_intents_only_cross_at_the_end() {
            Profile wantsRelationship = TestPeople.person().intent(Intent.RELATIONSHIP).build();
            Profile wantsCasual = TestPeople.person().intent(Intent.CASUAL).build();

            assertThat(Filters.passesSoft(wantsRelationship, wantsCasual, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(wantsRelationship, wantsCasual, ctx, 1)).isTrue();
        }
    }

    @Nested
    class Relaxation {

        @Test
        void grows_with_the_days_and_stops_at_three() {
            assertThat(Filters.relaxationForDaysWaiting(0)).isZero();
            assertThat(Filters.relaxationForDaysWaiting(1)).isCloseTo(0.33, org.assertj.core.data.Offset.offset(0.01));
            assertThat(Filters.relaxationForDaysWaiting(3)).isEqualTo(1.0);
            assertThat(Filters.relaxationForDaysWaiting(40)).isEqualTo(1.0);
        }

        @Test
        void whoever_has_waited_longest_of_the_two_rules() {
            Profile ana = TestPeople.anyone();
            Profile carlos = TestPeople.anyone();
            MatchContext oneDesperate =
                    MatchContext.on(TODAY)
                            .waiting(ana.accountId(), 0)
                            .waiting(carlos.accountId(), 3)
                            .build();

            assertThat(Filters.relaxationFor(ana, carlos, oneDesperate)).isEqualTo(1.0);
        }

        @Test
        void the_required_language_level_drops_but_has_a_floor() {
            assertThat(Filters.requiredLanguageLevel(0)).isEqualTo(0.70);
            assertThat(Filters.requiredLanguageLevel(1)).isEqualTo(0.35);
            assertThat(Filters.requiredLanguageLevel(5)).isEqualTo(0.35);
        }
    }
}
