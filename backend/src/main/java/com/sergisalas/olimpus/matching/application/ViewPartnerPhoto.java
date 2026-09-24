package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.UnlockLadder;
import com.sergisalas.olimpus.matching.domain.UnlockLevel;
import com.sergisalas.olimpus.profile.application.ViewPhoto;
import com.sergisalas.olimpus.profile.domain.PhotoNotVisibleException;
import java.time.Clock;
import java.util.UUID;

/**
 * Use case: seeing the other person's photo.
 *
 * <p>This is the check the whole photo design rests on. There is no link to a
 * photo anywhere; every single read comes through here and asks again: is this
 * conversation at level 3, and is the person asking really in it?
 *
 * <p>So a photo that was legitimately seen once stops being readable the moment
 * the level no longer holds.
 */
public class ViewPartnerPhoto {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ViewPhoto photos;
    private final Clock clock;

    public ViewPartnerPhoto(
            ConversationRepository conversations,
            MessageRepository messages,
            ViewPhoto photos,
            Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.photos = photos;
        this.clock = clock;
    }

    public ViewPhoto.Bytes execute(UUID conversationId, UUID viewer) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);
        if (!conversation.involves(viewer)) {
            throw new NotYourConversationException();
        }

        UnlockLevel level =
                UnlockLadder.levelOf(
                        conversation, messages.byConversation(conversationId), clock.instant());
        if (!level.atLeast(UnlockLevel.GOOD_CONNECTION)) {
            throw new PhotoNotVisibleException();
        }

        return photos.photoOf(conversation.partnerOf(viewer));
    }
}
