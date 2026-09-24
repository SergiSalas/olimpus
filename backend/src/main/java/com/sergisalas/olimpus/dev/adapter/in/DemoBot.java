package com.sergisalas.olimpus.dev.adapter.in;

import com.sergisalas.olimpus.chat.adapter.in.ChatBroadcaster;
import com.sergisalas.olimpus.chat.application.GetChat;
import com.sergisalas.olimpus.chat.application.LikeMessage;
import com.sergisalas.olimpus.chat.application.SendMessage;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.matching.application.AskToSeePhoto;
import com.sergisalas.olimpus.matching.application.DecideOnPartner;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.Decision;
import com.sergisalas.olimpus.matching.domain.UnlockLadder;
import com.sergisalas.olimpus.notifications.application.Announce;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Plays the other side of every test conversation, through the same use cases a
 * phone would call: it answers, puts a heart on every third message of yours,
 * asks to see you as soon as the rules allow and says yes at the end. That way every level can be reached alone.
 *
 * <p>It answers anything with canned lines: it is there to test the flow, not to
 * hold a conversation.
 */
@Component
@ConditionalOnProperty(name = "olimpus.dev-endpoints", havingValue = "true")
public class DemoBot {

    private static final Logger log = LoggerFactory.getLogger(DemoBot.class);

    /** A short pause, with "writing…" on your screen, before the answer lands. */
    private static final Duration THINKING = Duration.ofMillis(2500);

    private static final List<String> LINES =
            List.of(
                    "Jaja, cuéntame más 😄",
                    "¿En serio? No me lo esperaba",
                    "Me encanta eso 🌟",
                    "Vale, apuntado 📝",
                    "¿Y qué planes tienes para el finde?",
                    "Jajaja me has hecho reír",
                    "Eso suena genial",
                    "¿Te gustan los documentales? 🐙");

    private final JdbcTemplate jdbc;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final SendMessage sendMessage;
    private final LikeMessage likeMessage;
    private final GetChat getChat;
    private final AskToSeePhoto askToSeePhoto;
    private final DecideOnPartner decideOnPartner;
    private final ChatBroadcaster broadcaster;
    private final Announce announce;
    private final Clock clock;

    public DemoBot(
            JdbcTemplate jdbc,
            ConversationRepository conversations,
            MessageRepository messages,
            SendMessage sendMessage,
            LikeMessage likeMessage,
            GetChat getChat,
            AskToSeePhoto askToSeePhoto,
            DecideOnPartner decideOnPartner,
            ChatBroadcaster broadcaster,
            Announce announce,
            Clock clock) {
        this.jdbc = jdbc;
        this.conversations = conversations;
        this.messages = messages;
        this.sendMessage = sendMessage;
        this.likeMessage = likeMessage;
        this.getChat = getChat;
        this.askToSeePhoto = askToSeePhoto;
        this.decideOnPartner = decideOnPartner;
        this.broadcaster = broadcaster;
        this.announce = announce;
        this.clock = clock;
    }

    private record Seat(UUID conversationId, UUID bot) {}

    @Scheduled(fixedDelay = 1000)
    public void play() {
        List<Seat> seats =
                jdbc.query(
                        """
                        select c.id, a.id as bot
                        from conversation c
                        join account a on a.id in (c.account_a, c.account_b)
                        where c.state in ('OPEN', 'CONNECTED') and a.email like ?
                        """,
                        (rs, i) -> new Seat(rs.getObject("id", UUID.class), rs.getObject("bot", UUID.class)),
                        "%@" + DemoController.DOMAIN);

        for (Seat seat : seats) {
            try {
                playIn(seat);
            } catch (RuntimeException e) {
                // One stuck conversation must not stop the bot in the others.
                log.warn("Test bot could not play in {}: {}", seat.conversationId(), e.getMessage());
            }
        }
    }

    private void playIn(Seat seat) {
        Instant now = clock.instant();
        Conversation conversation = conversations.byId(seat.conversationId()).orElseThrow();
        List<Message> thread = messages.byConversation(conversation.id());

        if (!thread.isEmpty()) {
            Message last = thread.getLast();
            if (!last.senderAccountId().equals(seat.bot())) {
                if (last.sentAt().plus(THINKING).isBefore(now)) {
                    answer(conversation, seat.bot(), thread.size());
                    if (thread.size() % 3 == 0) heart(conversation, seat.bot(), last);
                } else if (conversation.acceptsMessagesAt(now)) {
                    broadcaster.typing(conversation, seat.bot());
                }
            }
        }

        if (!conversation.isOpen()) return;

        boolean botAsked =
                (seat.bot().equals(conversation.accountA())
                                ? conversation.photoWantedByA()
                                : conversation.photoWantedByB())
                        != null;
        if (!botAsked && UnlockLadder.canAskForPhoto(conversation, thread, now)) {
            askToSeePhoto.execute(conversation.id(), seat.bot());
        }

        if (conversation.acceptsDecisionAt(now) && conversation.decisionBy(seat.bot()) == null) {
            decideOnPartner.execute(conversation.id(), seat.bot(), Decision.YES);
        }
    }

    private void heart(Conversation conversation, UUID bot, Message yours) {
        var liked = likeMessage.execute(conversation.id(), bot, yours.id(), true);
        broadcaster.liked(liked.conversation(), yours.id(), true);
    }

    /** Same steps as the chat endpoint, so the phone sees it live. */
    private void answer(Conversation conversation, UUID bot, int count) {
        var sent = sendMessage.execute(conversation.id(), bot, LINES.get(count % LINES.size()));
        var chat = getChat.execute(conversation.id(), bot);
        broadcaster.newMessage(sent.conversation(), sent.message(), chat.partner().level());
        announce.newMessage(sent.conversation(), bot);
    }
}
