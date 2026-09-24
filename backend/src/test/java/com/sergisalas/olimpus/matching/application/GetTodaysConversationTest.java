package com.sergisalas.olimpus.matching.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.TestPeople;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetTodaysConversationTest {

    private FakeRoundWorld world;

    @BeforeEach
    void setUp() {
        world = new FakeRoundWorld();
    }

    @Test
    void without_a_conversation_it_returns_nothing_but_does_not_fail() {
        world.people.addAll(TestPeople.population(4, 1));

        var today = world.todaysConversation().execute(world.people.get(0).accountId());

        assertThat(today.conversation()).isEmpty();
        assertThat(today.partner()).isEmpty();
    }

    @Test
    void of_the_other_person_only_age_two_interests_and_distance_are_visible() {
        Profile ana =
                TestPeople.person()
                        .nickname("Ana")
                        .gender(Gender.WOMAN)
                        .seeking(Gender.MAN)
                        .age(30)
                        .at(41.3874, 2.1686)
                        .interests("movies", "climbing", "wine", "theatre", "running")
                        .build();
        Profile leo =
                TestPeople.person()
                        .nickname("Leo")
                        .gender(Gender.MAN)
                        .seeking(Gender.WOMAN)
                        .age(33)
                        .at(41.4036, 2.1744)
                        .interests("climbing", "movies", "podcasts", "surfing", "chess")
                        .build();
        world.people.addAll(List.of(ana, leo));
        world.dailyRound().execute(TestPeople.TODAY, RoundKind.MAIN);
        world.now = Instant.parse("2026-09-12T08:00:00Z");

        var today = world.todaysConversation().execute(ana.accountId());

        assertThat(today.conversation()).isPresent();
        var view = today.partner().orElseThrow();
        assertThat(view.age()).isEqualTo(33);
        assertThat(view.level()).isZero();
        assertThat(view.interestsShown()).hasSize(2);
        assertThat(view.approxDistanceKm()).isBetween(1, 4);

        // The two visible interests are among the shared ones: they are what
        // gives them something to talk about.
        assertThat(today.sharedInterests()).contains("movies", "climbing");
        assertThat(view.interestsShown()).allMatch(today.sharedInterests()::contains);
    }

    @Test
    void the_level_zero_view_does_not_leak_nickname_or_bio() {
        // The view grows with the level (PartnerViewTest covers each rung). What
        // this one guards is the card outside the chat: a freshly handed out
        // conversation, where nothing has been earned yet.
        Profile ana =
                TestPeople.person().nickname("Ana").gender(Gender.WOMAN).seeking(Gender.MAN).build();
        Profile leo =
                TestPeople.person().nickname("Leo").gender(Gender.MAN).seeking(Gender.WOMAN).build();
        world.people.addAll(List.of(ana, leo));
        world.dailyRound().execute(TestPeople.TODAY, RoundKind.MAIN);

        var view = world.todaysConversation().execute(ana.accountId()).partner().orElseThrow();

        assertThat(view.level()).isZero();
        assertThat(view.nickname()).isNull();
        assertThat(view.prompts()).isEmpty();
        assertThat(view.languages()).isEmpty();
        assertThat(view.intent()).isNull();
        assertThat(view.photoAvailable()).isFalse();
    }

    @Test
    void each_one_sees_the_other_not_themselves() {
        Profile ana = TestPeople.person().nickname("Ana").gender(Gender.WOMAN).seeking(Gender.MAN).age(30).build();
        Profile leo = TestPeople.person().nickname("Leo").gender(Gender.MAN).seeking(Gender.WOMAN).age(44).build();
        world.people.addAll(List.of(ana, leo));
        world.dailyRound().execute(TestPeople.TODAY, RoundKind.MAIN);

        var anasView = world.todaysConversation().execute(ana.accountId());
        var leosView = world.todaysConversation().execute(leo.accountId());

        assertThat(anasView.partner().orElseThrow().age()).isEqualTo(44);
        assertThat(leosView.partner().orElseThrow().age()).isEqualTo(30);
    }
}
