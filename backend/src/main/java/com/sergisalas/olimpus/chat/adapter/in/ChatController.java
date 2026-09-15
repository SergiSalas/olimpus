package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.chat.application.GetChat;
import com.sergisalas.olimpus.matching.application.AskToSeePhoto;
import com.sergisalas.olimpus.matching.application.ViewPartnerPhoto;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /**
     * Only what the level allows travels. A field that is null is not missing: it
     * has simply not been earned yet.
     */
    public record PartnerResponse(
            int level,
            int age,
            List<String> interests,
            int approxDistanceKm,
            String nickname,
            String bio,
            List<String> languages,
            String intent,
            boolean photoAvailable) {}

    public record ChatResponse(
            UUID conversationId,
            String state,
            Instant closesAt,
            String icebreaker,
            PartnerResponse partner,
            List<String> sharedInterests,
            boolean bothHaveWritten,
            boolean canAskForPhoto,
            boolean alreadyAskedForPhoto,
            List<MessageResponse> messages) {}

    public record PhotoAnswer(boolean bothAccepted, int level) {}

    public record SendRequest(String text) {}

    private final GetChat getChat;
    private final SendMessage sendMessage;
    private final ChatBroadcaster broadcaster;
    private final IcebreakerWording icebreakers;
    private final AskToSeePhoto askToSeePhoto;
    private final ViewPartnerPhoto viewPartnerPhoto;

    public ChatController(
            GetChat getChat,
            SendMessage sendMessage,
            ChatBroadcaster broadcaster,
            IcebreakerWording icebreakers,
            AskToSeePhoto askToSeePhoto,
            ViewPartnerPhoto viewPartnerPhoto) {
        this.getChat = getChat;
        this.sendMessage = sendMessage;
        this.broadcaster = broadcaster;
        this.icebreakers = icebreakers;
        this.askToSeePhoto = askToSeePhoto;
        this.viewPartnerPhoto = viewPartnerPhoto;
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
                        chat.partner().level(),
                        chat.partner().age(),
                        chat.partner().interestsShown(),
                        chat.partner().approxDistanceKm(),
                        chat.partner().nickname(),
                        chat.partner().bio(),
                        chat.partner().languages(),
                        chat.partner().intent() == null ? null : chat.partner().intent().name(),
                        chat.partner().photoAvailable()),
                chat.sharedInterests(),
                conversation.bothHaveWritten(),
                chat.canAskForPhoto(),
                chat.alreadyAsked(),
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

    /**
     * "I want to see you". The answer only says whether BOTH accepted: never
     * whether the other one had already asked.
     */
    @PostMapping("/see-photo")
    public PhotoAnswer seePhoto(@PathVariable UUID id, @CurrentAccount Account account) {
        var answer = askToSeePhoto.execute(id, account.id());
        return new PhotoAnswer(answer.bothAccepted(), answer.level().number());
    }

    /**
     * The other person's photo. There is no link: this is checked on every single
     * read, so it stops working the moment the level no longer holds.
     */
    @GetMapping("/partner-photo")
    public ResponseEntity<byte[]> partnerPhoto(
            @PathVariable UUID id, @CurrentAccount Account account) {
        var photo = viewPartnerPhoto.execute(id, account.id());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
    }

    private static MessageResponse toResponse(Message message, UUID viewer) {
        return new MessageResponse(
                message.id(),
                message.senderAccountId().equals(viewer),
                message.text(),
                message.sentAt());
    }
}
