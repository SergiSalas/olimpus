package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.chat.application.GetChat;
import com.sergisalas.olimpus.chat.application.LikeMessage;
import com.sergisalas.olimpus.matching.application.AskToSeePhoto;
import com.sergisalas.olimpus.notifications.application.Announce;
import com.sergisalas.olimpus.shared.adapter.Messages;
import com.sergisalas.olimpus.matching.application.DecideOnPartner;
import com.sergisalas.olimpus.matching.domain.Decision;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.sergisalas.olimpus.matching.application.ViewPartnerPhoto;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.sergisalas.olimpus.chat.application.SendMessage;
import com.sergisalas.olimpus.chat.application.SignalTyping;
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
    public record MessageResponse(UUID id, boolean mine, String text, Instant sentAt, boolean liked) {}

    public record LikeRequest(boolean liked) {}

    public record LikeResponse(UUID messageId, boolean liked) {}

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
            List<PromptResponse> prompts,
            List<String> languages,
            String intent,
            String genderLabel,
            String occupation,
            String fromPlace,
            boolean photoAvailable) {}

    /** The question already worded in the reader's language, with the answer. */
    public record PromptResponse(String question, String label, String answer) {}

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
            /** Whether the end-of-day question is on screen right now. */
            boolean decisionTime,
            /** What you answered, if you did. The other one's answer never travels. */
            String yourDecision,
            boolean connected,
            List<UnlockResponse> unlocks,
            List<MessageResponse> messages) {}

    public record PhotoAnswer(boolean bothAccepted, int level) {}

    /**
     * A notice to show inside the conversation, right after the message that
     * earned it.
     */
    public record UnlockResponse(int level, UUID afterMessageId, Instant at, String text) {}

    public record DecisionRequest(Decision answer) {}

    public record SendRequest(String text) {}

    private final GetChat getChat;
    private final SendMessage sendMessage;
    private final LikeMessage likeMessage;
    private final SignalTyping signalTyping;
    private final ChatBroadcaster broadcaster;
    private final IcebreakerWording icebreakers;
    private final AskToSeePhoto askToSeePhoto;
    private final ViewPartnerPhoto viewPartnerPhoto;
    private final DecideOnPartner decideOnPartner;
    private final Announce announce;
    private final Messages messages;

    public ChatController(
            GetChat getChat,
            SendMessage sendMessage,
            LikeMessage likeMessage,
            SignalTyping signalTyping,
            ChatBroadcaster broadcaster,
            IcebreakerWording icebreakers,
            AskToSeePhoto askToSeePhoto,
            ViewPartnerPhoto viewPartnerPhoto,
            DecideOnPartner decideOnPartner,
            Announce announce,
            Messages messages) {
        this.getChat = getChat;
        this.sendMessage = sendMessage;
        this.likeMessage = likeMessage;
        this.signalTyping = signalTyping;
        this.broadcaster = broadcaster;
        this.icebreakers = icebreakers;
        this.askToSeePhoto = askToSeePhoto;
        this.viewPartnerPhoto = viewPartnerPhoto;
        this.decideOnPartner = decideOnPartner;
        this.announce = announce;
        this.messages = messages;
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
                        chat.partner().prompts().stream()
                                .map(
                                        prompt ->
                                                new PromptResponse(
                                                        prompt.question(),
                                                        messages.promptLabel(prompt.question()),
                                                        prompt.answer()))
                                .toList(),
                        chat.partner().languages(),
                        chat.partner().intent() == null ? null : chat.partner().intent().name(),
                        chat.partner().genderLabel(),
                        chat.partner().occupation(),
                        chat.partner().fromPlace(),
                        chat.partner().photoAvailable()),
                chat.sharedInterests(),
                conversation.bothHaveWritten(),
                chat.canAskForPhoto(),
                chat.alreadyAsked(),
                chat.decisionTime(),
                chat.yourDecision() == null ? null : chat.yourDecision().name(),
                conversation.isConnected(),
                chat.unlocks().stream()
                        .map(
                                unlock ->
                                        new UnlockResponse(
                                                unlock.level().number(),
                                                unlock.afterMessageId(),
                                                unlock.at(),
                                                messages.get(
                                                        "unlock.level-" + unlock.level().number())))
                        .toList(),
                chat.messages().stream()
                        .map(m -> toResponse(m, account.id(), chat.liked().contains(m.id())))
                        .toList());
    }

    @PostMapping("/messages")
    public MessageResponse send(
            @PathVariable UUID id, @CurrentAccount Account account, @RequestBody SendRequest body) {

        var sent = sendMessage.execute(id, account.id(), body.text());

        // The writer gets the answer from the POST itself; the other person gets
        // it over the long-lived connection, if they have it open.
        // El nivel se calcula aqui y viaja con el mensaje: asi el desbloqueo se
        // ve en el momento tambien para quien lo recibe.
        var chat = getChat.execute(id, account.id());
        broadcaster.newMessage(
                sent.conversation(), sent.message(), chat.partner().level());
        // Y si tiene la app cerrada, le llega igual.
        announce.newMessage(sent.conversation(), account.id());

        return toResponse(sent.message(), account.id(), false);
    }

    /**
     * "I'm writing". The phone sends it at most every few seconds while typing;
     * it goes over HTTP like everything a phone sends, so the rules are checked
     * in one place, and comes down to the other person over the live connection.
     */
    @PostMapping("/typing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void typing(@PathVariable UUID id, @CurrentAccount Account account) {
        broadcaster.typing(signalTyping.execute(id, account.id()), account.id());
    }

    /**
     * A heart on one of the other person's messages, or taking it off. The other
     * person sees it land at once, over the long-lived connection.
     */
    @PostMapping("/messages/{messageId}/like")
    public LikeResponse like(
            @PathVariable UUID id,
            @PathVariable UUID messageId,
            @CurrentAccount Account account,
            @RequestBody LikeRequest body) {
        var result = likeMessage.execute(id, account.id(), messageId, body.liked());
        broadcaster.liked(result.conversation(), messageId, result.liked());
        return new LikeResponse(messageId, result.liked());
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

    /**
     * The end-of-day answer. It returns nothing: the result is put together at
     * 22:00, so not even the timing of this call can leak it.
     */
    @PostMapping("/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decide(
            @PathVariable UUID id,
            @CurrentAccount Account account,
            @RequestBody DecisionRequest body) {
        decideOnPartner.execute(id, account.id(), body.answer());
    }

    private static MessageResponse toResponse(Message message, UUID viewer, boolean liked) {
        return new MessageResponse(
                message.id(),
                message.senderAccountId().equals(viewer),
                message.text(),
                message.sentAt(),
                liked);
    }
}
