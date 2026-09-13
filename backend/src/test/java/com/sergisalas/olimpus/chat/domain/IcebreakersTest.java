package com.sergisalas.olimpus.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.TestPeople;
import com.sergisalas.olimpus.profile.domain.Profile;
import org.junit.jupiter.api.Test;

class IcebreakersTest {

    @Test
    void the_question_comes_from_something_both_picked() {
        Profile ana = TestPeople.person().interests("climbing", "movies", "wine", "theatre", "running").build();
        Profile leo = TestPeople.person().interests("climbing", "surfing", "chess", "art", "reading").build();

        assertThat(Icebreakers.rarestShared(ana, leo)).contains("climbing");
    }

    @Test
    void among_several_shared_interests_the_rarest_wins() {
        // Half the world picks "travel"; almost nobody picks "kendo". Asking
        // about the common one gives no conversation.
        Profile ana = TestPeople.person().interests("travel", "movies", "kendo", "music", "tv-series").build();
        Profile leo = TestPeople.person().interests("travel", "movies", "kendo", "reading", "running").build();

        assertThat(Icebreakers.rarestShared(ana, leo)).contains("kendo");
    }

    @Test
    void both_get_exactly_the_same_interest() {
        Profile ana = TestPeople.person().interests("astronomy", "movies", "wine", "theatre", "running").build();
        Profile leo = TestPeople.person().interests("astronomy", "movies", "surfing", "art", "reading").build();

        assertThat(Icebreakers.rarestShared(ana, leo)).isEqualTo(Icebreakers.rarestShared(leo, ana));
    }

    @Test
    void with_nothing_in_common_there_is_no_interest() {
        Profile ana = TestPeople.person().interests("kendo", "beekeeping", "ceramics", "diving", "instrument-making").build();
        Profile leo = TestPeople.person().interests("movies", "running", "wine", "art", "reading").build();

        assertThat(Icebreakers.rarestShared(ana, leo)).isEmpty();
    }
}
