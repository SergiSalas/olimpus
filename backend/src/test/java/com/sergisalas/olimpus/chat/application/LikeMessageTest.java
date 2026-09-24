package com.sergisalas.olimpus.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageLikes;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Origin;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LikeMessageTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LEO = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    private static final Instant MIDDAY = Instant.parse("2026-09-13T10:00:00Z");
    private static final Instant CLOSING = Instant.parse("2026-09-13T20:00:00Z");

    private Instant now = MIDDAY;
    private Conversation chat;
    private Message fromAna;
    private final Map<UUID, Instant> hearts = new HashMap<>();
    private LikeMessage like;

    @BeforeEach
    void setUp() {
        chat =
                new Conversation(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 9, 13),
                        RoundKind.MAIN,
                        ANA,
                        LEO,
                        Origin.BEST_MATCH,
                        0.8,
                        Instant.parse("2026-09-13T02:00:00Z"),
                        CLOSING,
                        ConversationState.OPEN,
                        1,
                        0,
                        "climbing",
                        null,
                        null,
                        null,
                        null);
        fromAna = Message.written(chat.id(), ANA, "Climbing gym, almost always", MIDDAY);

        ConversationRepository conversations =
                new ConversationRepository() {
                    @Override
                    public void save(Conversation conversation) {}

                    @Override
                    public List<Conversation> byDate(LocalDate date) {
                        return List.of(chat);
                    }

                    @Override
                    public Optional<Conversation> openFor(UUID accountId, LocalDate date) {
                        return Optional.of(chat);
                    }

                    @Override
                    public List<Conversation> connectionsOf(UUID accountId) {
                        return List.of();
                    }

                    @Override
                    public Optional<Conversation> byId(UUID id) {
                        return chat.id().equals(id) ? Optional.of(chat) : Optional.empty();
                    }
                };

        MessageRepository messages =
                new MessageRepository() {
                    @Override
                    public void save(Message message) {}

                    @Override
                    public List<Message> byConversation(UUID conversationId) {
                        return chat.id().equals(conversationId) ? List.of(fromAna) : List.of();
                    }
                };

        MessageLikes likes =
                new MessageLikes() {
                    @Override
                    public void set(UUID messageId, Instant likedAt) {
                        if (likedAt == null) hearts.remove(messageId);
                        else hearts.put(messageId, likedAt);
                    }

                    @Override
                    public Set<UUID> likedIn(UUID conversationId) {
                        return hearts.keySet();
                    }
                };

        Clock clock =
                new Clock() {
                    @Override
                    public java.time.ZoneId getZone() {
                        return ZoneOffset.UTC;
                    }

                    @Override
                    public Clock withZone(java.time.ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        return now;
                    }
                };

        like = new LikeMessage(conversations, messages, likes, clock);
    }

    @Test
    void the_other_person_puts_a_heart_and_can_take_it_off() {
        var on = like.execute(chat.id(), LEO, fromAna.id(), true);
        assertThat(on.liked()).isTrue();
        assertThat(hearts).containsEntry(fromAna.id(), MIDDAY);

        like.execute(chat.id(), LEO, fromAna.id(), false);
        assertThat(hearts).isEmpty();
    }

    @Test
    void nobody_hearts_their_own_message() {
        assertThatThrownBy(() -> like.execute(chat.id(), ANA, fromAna.id(), true))
                .isInstanceOf(RuleViolationException.class);
        assertThat(hearts).isEmpty();
    }

    @Test
    void a_stranger_cannot_heart_anything() {
        assertThatThrownBy(() -> like.execute(chat.id(), STRANGER, fromAna.id(), true))
                .isInstanceOf(NotYourConversationException.class);
    }

    @Test
    void a_message_from_another_chat_is_treated_as_not_there() {
        assertThatThrownBy(() -> like.execute(chat.id(), LEO, UUID.randomUUID(), true))
                .isInstanceOf(NotYourConversationException.class);
    }

    @Test
    void after_closing_time_no_more_hearts() {
        now = CLOSING.plusSeconds(1);
        assertThatThrownBy(() -> like.execute(chat.id(), LEO, fromAna.id(), true))
                .isInstanceOf(ChatClosedException.class);
        assertThat(hearts).isEmpty();
    }
}
