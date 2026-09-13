package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.matching.domain.Conversation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Who has the chat open right now, and how a message reaches them instantly.
 *
 * <p>It lives in memory on purpose: if the server restarts, phones reconnect
 * and fetch the conversation again. What really has to be kept is the messages,
 * and those are in the database.
 */
@Component
public class ChatBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ChatBroadcaster.class);

    /** Account -> open sessions (the app may be open in two places). */
    private final Map<UUID, Set<WebSocketSession>> open = new ConcurrentHashMap<>();

    public void register(UUID accountId, WebSocketSession session) {
        open.computeIfAbsent(accountId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(UUID accountId, WebSocketSession session) {
        Set<WebSocketSession> sessions = open.get(accountId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) open.remove(accountId);
    }

    public void newMessage(Conversation conversation, Message message) {
        for (UUID recipient : List.of(conversation.accountA(), conversation.accountB())) {
            boolean mine = recipient.equals(message.senderAccountId());
            send(
                    recipient,
                    """
                    {"type":"message","conversationId":"%s","id":"%s","mine":%s,"text":%s,"sentAt":"%s"}"""
                            .formatted(
                                    conversation.id(),
                                    message.id(),
                                    mine,
                                    asJson(message.text()),
                                    message.sentAt()));
        }
    }

    private void send(UUID accountId, String payload) {
        Set<WebSocketSession> sessions = open.get(accountId);
        if (sessions == null) return;

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (IOException e) {
                // One phone going away must not break delivery to the other.
                log.debug("Could not notify a session: {}", e.getMessage());
            }
        }
    }

    /** Minimal escaping, enough because only message text goes in. */
    private static String asJson(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
