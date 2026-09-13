package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso: escribir en la conversacion del dia.
 *
 * <p>Aqui viven las tres reglas que no pueden saltarse desde ninguna pantalla:
 * solo escriben los dos que estan dentro, solo mientras este abierta, y cada
 * mensaje suma a la cuenta de su lado.
 */
public class SendMessage {

    /** El mensaje guardado y como queda la conversacion despues. */
    public record Sent(Message message, Conversation conversation) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final Clock clock;

    public SendMessage(
            ConversationRepository conversations, MessageRepository messages, Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.clock = clock;
    }

    public Sent execute(UUID conversationId, UUID sender, String text) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);

        if (!conversation.involves(sender)) {
            throw new NotYourConversationException();
        }

        Instant now = clock.instant();
        if (!conversation.acceptsMessagesAt(now)) {
            throw new ChatClosedException(
                    conversation.isOpen()
                            ? "La conversación ya ha cerrado."
                            : "Esta conversación está cerrada.");
        }

        Message message = Message.written(conversationId, sender, text, now);
        messages.save(message);

        Conversation actualizada = conversation.withMessageFrom(sender);
        conversations.save(actualizada);

        return new Sent(message, actualizada);
    }
}
