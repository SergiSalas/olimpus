package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.chat.application.GetChat;
import com.sergisalas.olimpus.chat.application.SendMessage;
import com.sergisalas.olimpus.chat.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{id}")
public class ChatController {

    /** {@code mine} saves the phone from comparing ids. */
    public record MessageResponse(UUID id, boolean mine, String text, Instant sentAt) {}

    public record PartnerResponse(int age, List<String> interests, int approxDistanceKm, int level) {}

    public record ChatResponse(
            UUID conversationId,
            String state,
            Instant closesAt,
            String icebreaker,
            PartnerResponse partner,
            List<String> sharedInterests,
            boolean bothHaveWritten,
            List<MessageResponse> messages) {}

    public record SendRequest(String text) {}

    private final GetChat getChat;
    private final SendMessage sendMessage;
    private final ChatBroadcaster broadcaster;
    private final IcebreakerWording icebreakers;

    public ChatController(
            GetChat getChat,
            SendMessage sendMessage,
            ChatBroadcaster broadcaster,
            IcebreakerWording icebreakers) {
        this.getChat = getChat;
        this.sendMessage = sendMessage;
        this.broadcaster = broadcaster;
        this.icebreakers = icebreakers;
    }

    @GetMapping
    public ChatResponse chat(@PathVariable UUID id, @CurrentAccount Account account) {
        var chat = getChat.execute(id, account.id());
        var conversation = chat.conversation();

        return new ChatResponse(
                conversation.id(),
                conversation.state().name(),
                conversation.closesAt(),
                icebreakers.wordingFor(conversation.icebreakerInterest()),
                new PartnerResponse(
                        chat.partner().age(),
                        chat.partner().interestsShown(),
                        chat.partner().approxDistanceKm(),
                        chat.partner().level()),
                chat.sharedInterests(),
                conversation.bothHaveWritten(),
                chat.messages().stream().map(m -> toResponse(m, account.id())).toList());
    }

    @PostMapping("/messages")
    public MessageResponse send(
            @PathVariable UUID id, @CurrentAccount Account account, @RequestBody SendRequest body) {

        var sent = sendMessage.execute(id, account.id(), body.text());

        // The writer gets the answer from the POST itself; the other person gets
        // it over the long-lived connection, if they have it open.
        broadcaster.newMessage(sent.conversation(), sent.message());

        return toResponse(sent.message(), account.id());
    }

    private static MessageResponse toResponse(Message message, UUID viewer) {
        return new MessageResponse(
                message.id(),
                message.senderAccountId().equals(viewer),
                message.text(),
                message.sentAt());
    }
}
