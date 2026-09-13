package com.sergisalas.olimpus.chat.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** Someone tries to read or write in a conversation that is not theirs. */
public class NotYourConversationException extends RuntimeException implements UserFacingError {

    public NotYourConversationException() {
        super("conversation does not exist or is not the caller's");
    }

    @Override
    public String messageKey() {
        return "error.conversation.not-yours";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }
}
