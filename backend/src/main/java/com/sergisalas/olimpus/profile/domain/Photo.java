package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * The photo someone uploads at sign-up.
 *
 * <p>It is asked for on day one and shown on no day: nobody sees it until a
 * conversation reaches level 3 and <b>both</b> people accept. Asking later,
 * right at that moment, would break the moment.
 *
 * <p>Only the metadata lives here. The bytes are somewhere else, behind
 * {@link PhotoStorage}, because where they end up (a disk today, object storage
 * in the beta) must not change any rule.
 */
public record Photo(
        UUID accountId,
        String storageId,
        String contentType,
        long sizeBytes,
        ModerationState moderation,
        Instant uploadedAt) {

    /** 5 MB. Enough for a portrait; the phone shrinks it before sending anyway. */
    public static final long MAX_BYTES = 5 * 1024 * 1024;

    public static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");

    public Photo {
        if (accountId == null) throw new IllegalArgumentException("account is missing");
        if (storageId == null || storageId.isBlank()) {
            throw new IllegalArgumentException("storage id is missing");
        }
        if (moderation == null) throw new IllegalArgumentException("moderation state is missing");
        if (uploadedAt == null) throw new IllegalArgumentException("upload time is missing");

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuleViolationException("photo.type.unsupported");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_BYTES) {
            throw new RuleViolationException("photo.too-big", MAX_BYTES / (1024 * 1024));
        }
    }

    /** Only an approved photo can ever be shown to anyone. */
    public boolean canBeShown() {
        return moderation == ModerationState.APPROVED;
    }
}
