package com.sergisalas.olimpus.safety.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One person will not be shown to another again, ever.
 *
 * <p>A block is for good: the daily round reads it as a hard filter, in both
 * directions, and hard filters never give way no matter how long someone has
 * been waiting.
 *
 * @param reason null when it is only a block; filled in when it is also a report
 */
public record Block(UUID blocker, UUID blocked, ReportReason reason, Instant createdAt) {

    public Block {
        if (blocker == null || blocked == null) {
            throw new IllegalArgumentException("both people are required");
        }
        if (blocker.equals(blocked)) {
            throw new IllegalArgumentException("nobody blocks themselves");
        }
        if (createdAt == null) throw new IllegalArgumentException("the time is missing");
    }

    public boolean isReport() {
        return reason != null;
    }
}
