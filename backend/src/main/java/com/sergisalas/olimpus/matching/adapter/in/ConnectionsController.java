package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.matching.application.ListConnections;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The people you connected with: chats with no closing time. */
@RestController
@RequestMapping("/api/connections")
public class ConnectionsController {

    public record ConnectionResponse(
            UUID conversationId,
            int level,
            String nickname,
            int age,
            List<String> interests,
            boolean photoAvailable,
            Instant connectedOn,
            String lastMessage,
            Instant lastMessageAt) {}

    private final ListConnections listConnections;

    public ConnectionsController(ListConnections listConnections) {
        this.listConnections = listConnections;
    }

    @GetMapping
    public List<ConnectionResponse> connections(@CurrentAccount Account account) {
        return listConnections.execute(account.id()).stream()
                .map(
                        connection ->
                                new ConnectionResponse(
                                        connection.conversationId(),
                                        connection.partner().level(),
                                        connection.partner().nickname(),
                                        connection.partner().age(),
                                        connection.partner().interestsShown(),
                                        connection.partner().photoAvailable(),
                                        connection.connectedOn(),
                                        connection.lastMessage().map(m -> m.text()).orElse(null),
                                        connection.lastMessage().map(m -> m.sentAt()).orElse(null)))
                .toList();
    }
}
