package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** The app is only for people over 18. Not a preference: it is the door. */
public class UnderageException extends RuntimeException implements UserFacingError {

    public UnderageException() {
        super("underage: Olimpus is only for people aged 18 or over");
    }

    @Override
    public String messageKey() {
        return "error.profile.underage";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }

    @Override
    public Kind kind() {
        return Kind.NOT_ALLOWED;
    }
}
