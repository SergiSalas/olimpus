package com.sergisalas.olimpus.matching.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.TestPeople;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunDailyRoundTest {

    private static final LocalDate TODAY = TestPeople.TODAY;

    private FakeRoundWorld world;
    private RunDailyRound round;

    @BeforeEach
    void setUp() {
        world = new FakeRoundWorld();
        round = world.dailyRound();
    }

    @Test
    void the_round_creates_open_conversations_that_close_at_ten_at_night() {
        world.people.addAll(TestPeople.population(10, 1));

        var result = round.execute(TODAY, RoundKind.MAIN);

        assertThat(result.created()).isNotEmpty();
        for (Conversation c : result.created()) {
            assertThat(c.state()).isEqualTo(ConversationState.OPEN);
            assertThat(c.isSilent()).isTrue();
            // 4:00 and 22:00 in Madrid are 02:00 and 20:00 UTC (summer time).
            assertThat(c.opensAt()).isEqualTo(Instant.parse("2026-09-12T02:00:00Z"));
            assertThat(c.closesAt()).isEqualTo(Instant.parse("2026-09-12T20:00:00Z"));
        }
    }

    @Test
    void running_the_same_round_twice_does_not_match_twice() {
        world.people.addAll(TestPeople.population(10, 2));

        var first = round.execute(TODAY, RoundKind.MAIN);
        var second = round.execute(TODAY, RoundKind.MAIN);

        assertThat(first.alreadyRan()).isFalse();
        assertThat(second.alreadyRan()).isTrue();
        assertThat(second.created()).isEmpty();
        assertThat(world.stored).hasSize(first.created().size());
    }

    @Test
    void nobody_has_two_conversations_on_the_same_day() {
        world.people.addAll(TestPeople.population(30, 3));

        round.execute(TODAY, RoundKind.MAIN);
        var secondChance = round.execute(TODAY, RoundKind.SECOND_CHANCE);

        // In the second chance, whoever already has a live conversation stays
        // out. Nobody has written in this test, so all of them are silent and
        // get cancelled, and everyone goes back into the round.
        for (Profile person : world.people) {
            long open =
                    world.stored.values().stream()
                            .filter(c -> c.isOpen() && c.involves(person.accountId()))
                            .count();
            assertThat(open).as("%s has %d open conversations", person.nickname(), open)
                    .isLessThanOrEqualTo(1);
        }
        assertThat(secondChance.cancelled()).isNotEmpty();
    }

    @Test
    void the_second_chance_only_cancels_the_ones_still_silent() {
        world.people.addAll(TestPeople.population(20, 4));
        var main = round.execute(TODAY, RoundKind.MAIN);

        // One of them takes off: both write.
        Conversation talking = main.created().get(0);
        world.conversations.save(
                talking
                        .withMessageFrom(talking.accountA())
                        .withMessageFrom(talking.accountB()));

        var secondChance = round.execute(TODAY, RoundKind.SECOND_CHANCE);

        assertThat(secondChance.cancelled()).noneMatch(c -> c.id().equals(talking.id()));
        assertThat(world.stored.get(talking.id()).state()).isEqualTo(ConversationState.OPEN);
    }

    @Test
    void whoever_is_talking_does_not_enter_the_second_chance() {
        world.people.addAll(TestPeople.population(20, 5));
        var main = round.execute(TODAY, RoundKind.MAIN);

        Conversation talking = main.created().get(0);
        world.conversations.save(
                talking
                        .withMessageFrom(talking.accountA())
                        .withMessageFrom(talking.accountB()));

        var secondChance = round.execute(TODAY, RoundKind.SECOND_CHANCE);

        assertThat(secondChance.created())
                .noneMatch(
                        c -> c.involves(talking.accountA()) || c.involves(talking.accountB()));
    }

    @Test
    void the_second_chance_gives_a_conversation_to_whoever_was_left_out() {
        // Two women looking for men and one man: one of them is left without a pair.
        Profile ana = TestPeople.person().nickname("Ana").gender(Gender.WOMAN).seeking(Gender.MAN).build();
        Profile eva = TestPeople.person().nickname("Eva").gender(Gender.WOMAN).seeking(Gender.MAN).build();
        Profile leo = TestPeople.person().nickname("Leo").gender(Gender.MAN).seeking(Gender.WOMAN).build();
        world.people.addAll(java.util.List.of(ana, eva, leo));

        var main = round.execute(TODAY, RoundKind.MAIN);
        assertThat(main.created()).hasSize(1);
        assertThat(main.leftOut()).hasSize(1);

        var secondChance = round.execute(TODAY, RoundKind.SECOND_CHANCE);

        // The main one is cancelled for silence, so all three are back in the
        // second chance and the one left out gets another opportunity.
        assertThat(secondChance.cancelled()).hasSize(1);
        assertThat(secondChance.created()).hasSize(1);
    }

    @Test
    void with_nobody_signed_up_the_round_does_nothing() {
        var result = round.execute(TODAY, RoundKind.MAIN);

        assertThat(result.created()).isEmpty();
        assertThat(result.peopleInPool()).isZero();
        assertThat(world.stored).isEmpty();
    }

    @Test
    void the_same_round_on_two_different_days_gives_different_pairs() {
        world.people.addAll(TestPeople.population(40, 6));

        var today = round.execute(TODAY, RoundKind.MAIN);
        var tomorrow = round.execute(TODAY.plusDays(1), RoundKind.MAIN);

        assertThat(pairsOf(today)).isNotEqualTo(pairsOf(tomorrow));
    }

    @Test
    void the_opening_interest_is_the_rarest_one_they_share() {
        Profile ana =
                TestPeople.person()
                        .gender(Gender.WOMAN)
                        .seeking(Gender.MAN)
                        .interests("travel", "movies", "kendo", "music", "tv-series")
                        .build();
        Profile leo =
                TestPeople.person()
                        .gender(Gender.MAN)
                        .seeking(Gender.WOMAN)
                        .interests("travel", "movies", "kendo", "reading", "running")
                        .build();
        world.people.addAll(java.util.List.of(ana, leo));

        var result = round.execute(TODAY, RoundKind.MAIN);

        assertThat(result.created()).singleElement().extracting(Conversation::icebreakerInterest).isEqualTo("kendo");
    }

    private static java.util.Set<String> pairsOf(RunDailyRound.RoundResult result) {
        return result.created().stream()
                .map(c -> c.accountA() + "+" + c.accountB())
                .collect(java.util.stream.Collectors.toSet());
    }
}
