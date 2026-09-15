package com.sergisalas.olimpus.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageModerator;
import com.sergisalas.olimpus.chat.domain.MessageRejectedException;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendMessageTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID LEO = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    private static final Instant MIDDAY = Instant.parse("2026-09-13T10:00:00Z");
    private static final Instant CLOSING = Instant.parse("2026-09-13T20:00:00Z");

    private Instant now = MIDDAY;
    private final Map<UUID, Conversation> storedConversations = new LinkedHashMap<>();
    private final List<Message> storedMessages = new ArrayList<>();

    private Conversation chat;
    private SendMessage send;

    /** Moderation that lets everything through; one test swaps it for a strict one. */
    private MessageModerator moderator = text -> MessageModerator.Verdict.ALLOW;

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
                        "climbing",
                        null,
                        null,
                        null,
                        null);
        storedConversations.put(chat.id(), chat);

        ConversationRepository conversations =
                new ConversationRepository() {
                    @Override
                    public void save(Conversation conversation) {
                        storedConversations.put(conversation.id(), conversation);
                    }

                    @Override
                    public List<Conversation> byDate(LocalDate date) {
                        return storedConversations.values().stream()
                                .filter(c -> c.roundDate().equals(date))
                                .toList();
                    }

                    @Override
                    public Optional<Conversation> openFor(UUID accountId, LocalDate date) {
                        return storedConversations.values().stream()
                                .filter(c -> c.isOpen() && c.involves(accountId))
                                .findFirst();
                    }

                    @Override
                    public List<Conversation> connectionsOf(UUID accountId) {
                        return storedConversations.values().stream()
                                .filter(c -> c.isConnected() && c.involves(accountId))
                                .toList();
                    }

                    @Override
                    public Optional<Conversation> byId(UUID id) {
                        return Optional.ofNullable(storedConversations.get(id));
                    }
                };

        MessageRepository messages =
                new MessageRepository() {
                    @Override
                    public void save(Message message) {
                        storedMessages.add(message);
                    }

                    @Override
                    public List<Message> byConversation(UUID conversationId) {
                        return storedMessages.stream()
                                .filter(m -> m.conversationId().equals(conversationId))
                                .toList();
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

        send = new SendMessage(conversations, messages, moderator, clock);
    }

    @Test
    void writing_stores_the_message_and_adds_to_its_sides_count() {
        var sent = send.execute(chat.id(), ANA, "Climbing gym, almost always");

        assertThat(storedMessages).hasSize(1);
        assertThat(sent.conversation().messagesFromA()).isEqualTo(1);
        assertThat(sent.conversation().messagesFromB()).isZero();
        assertThat(sent.conversation().bothHaveWritten()).isFalse();
    }

    @Test
    void the_conversation_takes_off_when_both_have_written() {
        send.execute(chat.id(), ANA, "Hi!");
        send.execute(chat.id(), ANA, "How are you?");
        var after = send.execute(chat.id(), LEO, "Hey");

        assertThat(after.conversation().messagesFromA()).isEqualTo(2);
        assertThat(after.conversation().messagesFromB()).isEqualTo(1);
        assertThat(after.conversation().bothHaveWritten()).isTrue();
        assertThat(after.conversation().isSilent()).isFalse();
    }

    @Test
    void a_stranger_cannot_write_in_someone_elses_conversation() {
        assertThatThrownBy(() -> send.execute(chat.id(), STRANGER, "hello?"))
                .isInstanceOf(NotYourConversationException.class);

        assertThat(storedMessages).isEmpty();
    }

    @Test
    void a_conversation_that_does_not_exist_is_treated_like_someone_elses() {
        assertThatThrownBy(() -> send.execute(UUID.randomUUID(), ANA, "hello?"))
                .isInstanceOf(NotYourConversationException.class);
    }

    @Test
    void at_ten_at_night_nobody_can_write_any_more() {
        now = CLOSING;

        assertThatThrownBy(() -> send.execute(chat.id(), ANA, "still there?"))
                .isInstanceOfSatisfying(
                        ChatClosedException.class,
                        e -> assertThat(e.messageKey()).isEqualTo("error.chat.time-over"));

        assertThat(storedMessages).isEmpty();
    }

    @Test
    void nor_in_a_cancelled_conversation() {
        storedConversations.put(chat.id(), chat.cancelled());

        assertThatThrownBy(() -> send.execute(chat.id(), ANA, "hello"))
                .isInstanceOfSatisfying(
                        ChatClosedException.class,
                        e -> assertThat(e.messageKey()).isEqualTo("error.chat.closed"));
    }

    @Test
    void an_empty_or_whitespace_only_message_does_not_count() {
        assertThatThrownBy(() -> send.execute(chat.id(), ANA, "   "))
                .isInstanceOf(RuleViolationException.class);

        assertThat(storedMessages).isEmpty();
        assertThat(storedConversations.get(chat.id()).messagesFromA()).isZero();
    }

    @Test
    void a_huge_message_is_rejected() {
        assertThatThrownBy(() -> send.execute(chat.id(), ANA, "x".repeat(1001)))
                .isInstanceOf(RuleViolationException.class);
    }

    @Test
    void a_message_moderation_rejects_never_reaches_the_other_person() {
        moderator = text -> MessageModerator.Verdict.REJECT;
        setUp();

        assertThatThrownBy(() -> send.execute(chat.id(), ANA, "algo horrible"))
                .isInstanceOf(MessageRejectedException.class);

        assertThat(storedMessages).isEmpty();
        assertThat(storedConversations.get(chat.id()).messagesFromA()).isZero();
    }
}
