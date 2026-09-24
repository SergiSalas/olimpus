package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** No sign-up yet: the app sends the person to do it. */
public class ProfileNotFoundException extends RuntimeException implements UserFacingError {

    public ProfileNotFoundException() {
        super("profile not found");
    }

    @Override
    public String messageKey() {
        return "error.profile.not-found";
    }

    @Override
    public Object[] messageArgs() {
        return new Object[0];
    }

    @Override
    public Kind kind() {
        return Kind.NOT_FOUND;
    }
}
