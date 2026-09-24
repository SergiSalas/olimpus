package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** The conversation has not earned the photo yet: not enough back-and-forth, or not enough time. */
public class TooSoonForPhotoException extends RuntimeException implements UserFacingError {

    @Override
    public String messageKey() {
        return "error.photo.too-soon";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }

    @Override
    public Kind kind() {
        return Kind.WRONG_MOMENT;
    }
}
