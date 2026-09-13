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
 * Quien esta ahora mismo con el chat abierto, y como se le hace llegar un
 * mensaje al instante.
 *
 * <p>Vive en memoria a proposito: si el servidor se reinicia, los moviles se
 * reconectan y vuelven a pedir la conversacion. Lo que hay que guardar de
 * verdad son los mensajes, y esos estan en la base de datos.
 */
@Component
public class ChatBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ChatBroadcaster.class);

    /** Cuenta -> sesiones abiertas (puede tener la app en dos sitios). */
    private final Map<UUID, Set<WebSocketSession>> abiertas = new ConcurrentHashMap<>();

    public void register(UUID accountId, WebSocketSession session) {
        abiertas.computeIfAbsent(accountId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(UUID accountId, WebSocketSession session) {
        Set<WebSocketSession> sesiones = abiertas.get(accountId);
        if (sesiones == null) return;
        sesiones.remove(session);
        if (sesiones.isEmpty()) abiertas.remove(accountId);
    }

    public void newMessage(Conversation conversation, Message message) {
        for (UUID destinatario : List.of(conversation.accountA(), conversation.accountB())) {
            boolean suyo = destinatario.equals(message.senderAccountId());
            enviar(
                    destinatario,
                    """
                    {"type":"message","conversationId":"%s","id":"%s","mine":%s,"text":%s,"sentAt":"%s"}"""
                            .formatted(
                                    conversation.id(),
                                    message.id(),
                                    suyo,
                                    comoJson(message.text()),
                                    message.sentAt()));
        }
    }

    private void enviar(UUID accountId, String payload) {
        Set<WebSocketSession> sesiones = abiertas.get(accountId);
        if (sesiones == null) return;

        for (WebSocketSession session : sesiones) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (IOException e) {
                // Que un movil se haya ido no puede tumbar el envio al otro.
                log.debug("No se pudo avisar a una sesion: {}", e.getMessage());
            }
        }
    }

    /** Escapado minimo, suficiente porque solo metemos texto de mensajes. */
    private static String comoJson(String texto) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
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
