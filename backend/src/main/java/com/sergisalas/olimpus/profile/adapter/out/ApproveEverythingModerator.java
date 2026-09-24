package com.sergisalas.olimpus.profile.adapter.out;

import com.sergisalas.olimpus.profile.domain.ModerationState;
import com.sergisalas.olimpus.profile.domain.PhotoModerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development version: it approves everything and says so loudly.
 *
 * <p>Before the beta this has to be replaced by a real moderation service. The
 * warning is deliberately noisy so that nobody ships this by accident.
 */
@Component
public class ApproveEverythingModerator implements PhotoModerator {

    private static final Logger log = LoggerFactory.getLogger(ApproveEverythingModerator.class);

    @Override
    public ModerationState review(byte[] bytes, String contentType) {
        log.warn(
                "Photo approved WITHOUT moderation ({} bytes, {}). Development only: the beta must"
                        + " not open with this piece in place.",
                bytes.length,
                contentType);
        return ModerationState.APPROVED;
    }
}
