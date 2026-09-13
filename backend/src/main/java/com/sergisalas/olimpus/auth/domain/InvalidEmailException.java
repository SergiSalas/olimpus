package com.sergisalas.olimpus.auth.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;

public class InvalidEmailException extends RuleViolationException {

    public InvalidEmailException(String messageKey) {
        super(messageKey);
    }
}
