package com.sergisalas.olimpus.auth.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/**
 * The code is not valid: it does not exist, it expired, it does not match or
 * the attempts ran out. The reason is kept for logs, but whoever typed it is
 * always told the same thing, to give no hints to someone guessing codes.
 */
public class InvalidLoginCodeException extends RuntimeException implements UserFacingError {

    public InvalidLoginCodeException(String reason) {
        super(reason);
    }

    @Override
    public String messageKey() {
        return "error.login-code.invalid";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }
}
