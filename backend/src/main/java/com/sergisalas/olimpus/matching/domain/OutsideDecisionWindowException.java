package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.shared.domain.UserFacingError;

/** Too early (the question is asked in the last half hour) or already over. */
public class OutsideDecisionWindowException extends RuntimeException implements UserFacingError {

    @Override
    public String messageKey() {
        return "error.decision.outside-window";
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
