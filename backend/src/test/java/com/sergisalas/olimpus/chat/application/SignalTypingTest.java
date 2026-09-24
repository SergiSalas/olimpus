package com.sergisalas.olimpus.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Origin;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignalTypingTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LEO = UUID.randomUUID();
    private static final Instant CLOSING = Instant.parse("2026-09-13T20:00:00Z");

    private Instant now = Instant.parse("2026-09-13T10:00:00Z");
    private Conversation chat;
    private SignalTyping typing;

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
                        0,
                        0,
                        null,
                        null,
                        null,
                        null,
                        null);

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

        typing = new SignalTyping(conversations, clock);
    }

    @Test
    void someone_in_the_conversation_can_say_they_are_writing() {
        assertThat(typing.execute(chat.id(), ANA)).isEqualTo(chat);
    }

    @Test
    void a_stranger_cannot() {
        assertThatThrownBy(() -> typing.execute(chat.id(), UUID.randomUUID()))
                .isInstanceOf(NotYourConversationException.class);
    }

    @Test
    void not_after_closing_time() {
        now = CLOSING.plusSeconds(1);
        assertThatThrownBy(() -> typing.execute(chat.id(), ANA))
                .isInstanceOf(ChatClosedException.class);
    }
}
