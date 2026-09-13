package com.sergisalas.olimpus.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.application.FakeRoundWorld;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.TestPeople;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloseFinishedConversationsTest {

    private FakeRoundWorld world;
    private CloseFinishedConversations close;

    @BeforeEach
    void setUp() {
        world = new FakeRoundWorld();
        world.people.addAll(TestPeople.population(10, 1));
        world.dailyRound().execute(TestPeople.TODAY, RoundKind.MAIN);
        close = new CloseFinishedConversations(world.conversations, world.schedule, world.clock);
    }

    @Test
    void before_ten_nothing_is_closed() {
        world.now = Instant.parse("2026-09-12T19:59:00Z"); // 21:59 in Madrid

        assertThat(close.execute()).isEmpty();
        assertThat(world.stored.values()).allMatch(c -> c.state() == ConversationState.OPEN);
    }

    @Test
    void at_ten_sharp_they_all_close() {
        world.now = Instant.parse("2026-09-12T20:00:00Z"); // 22:00 in Madrid

        var closed = close.execute();

        assertThat(closed).isNotEmpty();
        assertThat(world.stored.values()).allMatch(c -> c.state() == ConversationState.CLOSED);
    }

    @Test
    void if_the_server_was_down_yesterdays_ones_are_closed_too() {
        // It wakes up at 10 in the morning of the next day.
        world.now = Instant.parse("2026-09-13T08:00:00Z");

        assertThat(close.execute()).isNotEmpty();
        assertThat(world.stored.values()).noneMatch(c -> c.state() == ConversationState.OPEN);
    }

    @Test
    void closing_twice_does_not_close_what_is_already_closed() {
        world.now = Instant.parse("2026-09-12T20:00:00Z");
        close.execute();

        assertThat(close.execute()).isEmpty();
    }
}
