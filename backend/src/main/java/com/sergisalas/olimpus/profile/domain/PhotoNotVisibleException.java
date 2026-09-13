package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/**
 * There is no photo to show: it was never uploaded, it did not pass moderation,
 * or whoever is asking has not earned it yet. The three cases answer the same
 * thing on purpose.
 */
public class PhotoNotVisibleException extends RuntimeException implements UserFacingError {

    @Override
    public String messageKey() {
        return "error.photo.not-visible";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }
}
