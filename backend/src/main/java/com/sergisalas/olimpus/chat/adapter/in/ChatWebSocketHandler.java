package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.auth.application.AuthenticateSession;
import com.sergisalas.olimpus.auth.domain.Account;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * The chat's long-lived connection. It is <b>receive only</b>: messages are sent
 * over HTTP, which already handles errors and retries, and only the other
 * person's messages come down here.
 *
 * <p>That way there is a single place where the rules for writing are checked.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String ACCOUNT = "account";

    private final AuthenticateSession authenticateSession;
    private final ChatBroadcaster broadcaster;

    public ChatWebSocketHandler(
            AuthenticateSession authenticateSession, ChatBroadcaster broadcaster) {
        this.authenticateSession = authenticateSession;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Optional<Account> account = authenticate(session);
        if (account.isEmpty()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid session"));
            return;
        }
        UUID accountId = account.get().id();
        session.getAttributes().put(ACCOUNT, accountId);
        broadcaster.register(accountId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object accountId = session.getAttributes().get(ACCOUNT);
        if (accountId instanceof UUID id) {
            broadcaster.unregister(id, session);
        }
    }

    /**
     * The token travels in the URL because WebSockets on the phone cannot set
     * headers. Since everything goes over TLS in production, it is not written
     * anywhere except in the server's own logs.
     */
    private Optional<Account> authenticate(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return Optional.empty();

        for (String part : uri.getQuery().split("&")) {
            if (part.startsWith("token=")) {
                return authenticateSession.execute(
                        URLDecoder.decode(part.substring("token=".length()), StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }
}
