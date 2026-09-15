package com.sergisalas.olimpus.chat.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** It is already 22:00, or the conversation was cancelled: no more writing. */
public class ChatClosedException extends RuntimeException implements UserFacingError {

    private final String messageKey;

    private ChatClosedException(String reason, String messageKey) {
        super(reason);
        this.messageKey = messageKey;
    }

    /** Still marked open, but its closing time has passed. */
    public static ChatClosedException timeOver() {
        return new ChatClosedException("closing time has passed", "error.chat.time-over");
    }

    /** Already closed or cancelled. */
    public static ChatClosedException notOpen() {
        return new ChatClosedException("conversation is not open", "error.chat.closed");
    }

    @Override
    public String messageKey() {
        return messageKey;
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
