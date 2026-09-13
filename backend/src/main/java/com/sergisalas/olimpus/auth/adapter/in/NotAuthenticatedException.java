package com.sergisalas.olimpus.auth.adapter.in;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

public class NotAuthenticatedException extends RuntimeException implements UserFacingError {

    public NotAuthenticatedException() {
        super("no valid session");
    }

    @Override
    public String messageKey() {
        return "error.not-authenticated";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }
}
