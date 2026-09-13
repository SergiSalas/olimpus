package com.sergisalas.olimpus.profile.application;

import com.sergisalas.olimpus.profile.domain.ModerationState;
import com.sergisalas.olimpus.profile.domain.Photo;
import com.sergisalas.olimpus.profile.domain.PhotoModerator;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import com.sergisalas.olimpus.profile.domain.PhotoStorage;
import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.Clock;
import java.util.UUID;

/**
 * Use case: uploading the photo.
 *
 * <p>Order matters here. The photo is moderated <b>before</b> it is stored as
 * someone's photo, so a rejected one never becomes visible for a single second.
 * And replacing a photo deletes the old bytes: keeping them would mean holding
 * pictures of people that nothing points to any more.
 */
public class UploadPhoto {

    private final PhotoRepository photos;
    private final PhotoStorage storage;
    private final PhotoModerator moderator;
    private final Clock clock;

    public UploadPhoto(
            PhotoRepository photos, PhotoStorage storage, PhotoModerator moderator, Clock clock) {
        this.photos = photos;
        this.storage = storage;
        this.moderator = moderator;
        this.clock = clock;
    }

    public Photo execute(UUID accountId, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new RuleViolationException("photo.empty");
        }
        if (contentType == null || !Photo.ALLOWED_TYPES.contains(contentType)) {
            throw new RuleViolationException("photo.type.unsupported");
        }
        if (bytes.length > Photo.MAX_BYTES) {
            throw new RuleViolationException("photo.too-big", Photo.MAX_BYTES / (1024 * 1024));
        }
        if (!looksLikeAnImage(bytes, contentType)) {
            // The declared type is not proof of anything: a text file renamed to
            // .jpg would arrive as image/jpeg.
            throw new RuleViolationException("photo.not-an-image");
        }

        ModerationState verdict = moderator.review(bytes, contentType);
        if (verdict == ModerationState.REJECTED) {
            throw new RuleViolationException("photo.rejected");
        }

        photos.findByAccountId(accountId).ifPresent(old -> storage.delete(old.storageId()));

        Photo photo =
                new Photo(
                        accountId,
                        storage.store(bytes, contentType),
                        contentType,
                        bytes.length,
                        verdict,
                        clock.instant());
        photos.save(photo);
        return photo;
    }

    /** The first bytes of the file have to match what it claims to be. */
    private static boolean looksLikeAnImage(byte[] bytes, String contentType) {
        if (bytes.length < 8) return false;

        boolean jpeg =
                (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        boolean png =
                (bytes[0] & 0xFF) == 0x89
                        && bytes[1] == 'P'
                        && bytes[2] == 'N'
                        && bytes[3] == 'G'
                        && (bytes[4] & 0xFF) == 0x0D
                        && (bytes[5] & 0xFF) == 0x0A;

        return contentType.equals("image/jpeg") ? jpeg : png;
    }
}
