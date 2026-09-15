package com.sergisalas.olimpus.chat.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** Moderation stopped the message. */
public class MessageRejectedException extends RuntimeException implements UserFacingError {

    @Override
    public String messageKey() {
        return "error.message.rejected";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }
}
