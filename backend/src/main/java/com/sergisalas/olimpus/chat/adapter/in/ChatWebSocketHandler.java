package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.auth.application.AuthenticateSession;
import com.sergisalas.olimpus.auth.domain.Account;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * La conexion permanente del chat. Es de <b>solo recibir</b>: los mensajes se
 * envian por HTTP, que ya tiene resueltos los errores y los reintentos, y por
 * aqui solo bajan los del otro.
 *
 * <p>Asi hay un unico sitio donde se comprueban las reglas de escribir.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String CUENTA = "cuenta";

    private final AuthenticateSession authenticateSession;
    private final ChatBroadcaster broadcaster;

    public ChatWebSocketHandler(
            AuthenticateSession authenticateSession, ChatBroadcaster broadcaster) {
        this.authenticateSession = authenticateSession;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Optional<Account> cuenta = autenticar(session);
        if (cuenta.isEmpty()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("sesion no valida"));
            return;
        }
        UUID accountId = cuenta.get().id();
        session.getAttributes().put(CUENTA, accountId);
        broadcaster.register(accountId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object accountId = session.getAttributes().get(CUENTA);
        if (accountId instanceof UUID id) {
            broadcaster.unregister(id, session);
        }
    }

    /**
     * La llave viaja en la direccion porque los WebSocket del movil no permiten
     * poner cabeceras. Al ir todo por TLS en produccion, no queda escrita en
     * ningun sitio salvo en los logs del propio servidor.
     */
    private Optional<Account> autenticar(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return Optional.empty();

        for (String parte : uri.getQuery().split("&")) {
            if (parte.startsWith("token=")) {
                return authenticateSession.execute(
                        java.net.URLDecoder.decode(
                                parte.substring("token=".length()),
                                java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }
}
