package com.sergisalas.olimpus.profile.application;

import com.sergisalas.olimpus.profile.domain.Photo;
import com.sergisalas.olimpus.profile.domain.PhotoNotVisibleException;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import java.util.UUID;

/**
 * Use case: reading someone's photo bytes.
 *
 * <p>Every read passes through here, and here the question is always the same:
 * may <i>this</i> person see <i>that</i> photo? Today only its owner may. From
 * step 6 on, so may whoever is in a conversation that reached level 3, and this
 * is the single place that will have to learn that.
 *
 * <p>There is no public link to a photo, not even a short-lived one. A link
 * that exists can be forwarded; a check cannot.
 */
public class ViewPhoto {

    /** Bytes plus the type, ready to be written to the response. */
    public record Bytes(byte[] content, String contentType) {}

    private final PhotoRepository photos;
    private final PhotoStorage storage;

    public ViewPhoto(PhotoRepository photos, PhotoStorage storage) {
        this.photos = photos;
        this.storage = storage;
    }

    public Bytes ownPhoto(UUID accountId) {
        Photo photo =
                photos.findByAccountId(accountId).orElseThrow(PhotoNotVisibleException::new);

        // The owner sees their own photo even while moderation is pending: they
        // need to know what they uploaded.
        return read(photo);
    }

    /** Whether someone has a photo that could be shown to others. */
    public boolean hasShowablePhoto(UUID accountId) {
        return photos.findByAccountId(accountId).filter(Photo::canBeShown).isPresent();
    }

    /**
     * Someone else's photo, once whoever asks has earned it. Deciding that is not
     * this class's business: the unlock level lives in the conversation, so the
     * caller has already checked it. What is checked here is the other half: a
     * photo that moderation has not approved is shown to nobody, ever.
     */
    public Bytes photoOf(UUID ownerId) {
        Photo photo =
                photos.findByAccountId(ownerId)
                        .filter(Photo::canBeShown)
                        .orElseThrow(PhotoNotVisibleException::new);
        return read(photo);
    }

    private Bytes read(Photo photo) {
        byte[] content =
                storage.read(photo.storageId()).orElseThrow(PhotoNotVisibleException::new);
        return new Bytes(content, photo.contentType());
    }
}
