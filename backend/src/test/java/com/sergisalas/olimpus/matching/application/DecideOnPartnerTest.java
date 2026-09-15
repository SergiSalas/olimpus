package com.sergisalas.olimpus.matching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.chat.application.CloseFinishedConversations;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Decision;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.matching.domain.OutsideDecisionWindowException;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.TestPeople;
import com.sergisalas.olimpus.matching.domain.UnlockLadder;
import com.sergisalas.olimpus.matching.domain.UnlockLevel;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The end of the day. What these tests guard is not the mechanics but the
 * promise: saying no has to cost nothing, and it only costs nothing while
 * nobody can find out who said it.
 */
class DecideOnPartnerTest {

    /** 22:00 in Madrid, the closing time of that day's conversation. */
    private static final Instant CLOSING = Instant.parse("2026-09-12T20:00:00Z");

    private static final Instant BEFORE_THE_QUESTION = CLOSING.minusSeconds(60 * 31);
    private static final Instant QUESTION_TIME = CLOSING.minusSeconds(60 * 20);

    private FakeRoundWorld world;
    private DecideOnPartner decide;
    private CloseFinishedConversations close;
    private Profile ana;
    private Profile leo;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        world = new FakeRoundWorld();
        ana = TestPeople.person().nickname("Ana").gender(Gender.WOMAN).seeking(Gender.MAN).build();
        leo = TestPeople.person().nickname("Leo").gender(Gender.MAN).seeking(Gender.WOMAN).build();
        world.people.addAll(List.of(ana, leo));

        var round = world.dailyRound().execute(TestPeople.TODAY, RoundKind.MAIN);
        conversationId = round.created().get(0).id();

        decide = new DecideOnPartner(world.conversations, world.clock);
        close = new CloseFinishedConversations(world.conversations, world.schedule, world.clock);
        world.now = QUESTION_TIME;
    }

    private Conversation current() {
        return world.conversations.byId(conversationId).orElseThrow();
    }

    @Test
    void the_question_is_not_asked_before_the_last_half_hour() {
        world.now = BEFORE_THE_QUESTION;

        assertThatThrownBy(() -> decide.execute(conversationId, ana.accountId(), Decision.YES))
                .isInstanceOf(OutsideDecisionWindowException.class);
    }

    @Test
    void nor_once_the_conversation_has_closed() {
        world.now = CLOSING;

        assertThatThrownBy(() -> decide.execute(conversationId, ana.accountId(), Decision.YES))
                .isInstanceOf(OutsideDecisionWindowException.class);
    }

    @Test
    void an_outsider_cannot_answer_for_anyone() {
        assertThatThrownBy(() -> decide.execute(conversationId, UUID.randomUUID(), Decision.NO))
                .isInstanceOf(NotYourConversationException.class);
    }

    @Test
    void your_answer_is_yours_and_the_other_one_never_sees_it() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);

        assertThat(current().decisionBy(ana.accountId())).isEqualTo(Decision.YES);
        assertThat(current().decisionBy(leo.accountId())).isNull();
    }

    @Test
    void you_can_change_your_mind_while_the_conversation_is_still_alive() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);
        decide.execute(conversationId, ana.accountId(), Decision.NO);

        assertThat(current().decisionBy(ana.accountId())).isEqualTo(Decision.NO);
    }

    @Test
    void two_yeses_become_a_connection_that_never_closes() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);
        decide.execute(conversationId, leo.accountId(), Decision.YES);
        world.now = CLOSING;

        var result = close.execute();

        assertThat(result.connected()).hasSize(1);
        assertThat(result.closed()).isEmpty();
        assertThat(current().state()).isEqualTo(ConversationState.CONNECTED);
        // And the chat stays writable, days later.
        assertThat(current().acceptsMessagesAt(CLOSING.plusSeconds(86400 * 3))).isTrue();
    }

    @Test
    void one_no_closes_it_and_the_ending_looks_the_same_from_both_sides() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);
        decide.execute(conversationId, leo.accountId(), Decision.NO);
        world.now = CLOSING;

        close.execute();

        assertThat(current().state()).isEqualTo(ConversationState.CLOSED);
        assertThat(current().acceptsMessagesAt(CLOSING.plusSeconds(60))).isFalse();
    }

    @Test
    void silence_is_not_a_yes() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);
        world.now = CLOSING;

        close.execute();

        assertThat(current().state()).isEqualTo(ConversationState.CLOSED);
    }

    @Test
    void a_connection_sits_at_level_four_and_does_not_take_up_the_daily_slot() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);
        decide.execute(conversationId, leo.accountId(), Decision.YES);
        world.now = CLOSING;
        close.execute();

        assertThat(UnlockLadder.levelOf(current(), List.of(), world.now))
                .isEqualTo(UnlockLevel.TRUST);

        // The next day they are both in the pool again, with someone new.
        var tomorrow = world.dailyRound().execute(TestPeople.TODAY.plusDays(1), RoundKind.MAIN);
        assertThat(tomorrow.peopleInPool()).isEqualTo(2);
    }

    @Test
    void connections_are_listed_apart_from_the_conversation_of_the_day() {
        decide.execute(conversationId, ana.accountId(), Decision.YES);
        decide.execute(conversationId, leo.accountId(), Decision.YES);
        world.now = CLOSING;
        close.execute();

        var connections =
                new ListConnections(
                                world.conversations,
                                world.messages,
                                world.profiles,
                                world.schedule,
                                world.clock)
                        .execute(ana.accountId());

        assertThat(connections).hasSize(1);
        assertThat(connections.get(0).partner().nickname()).isEqualTo("Leo");
        assertThat(connections.get(0).partner().photoAvailable()).isTrue();
    }
}
