package com.sergisalas.olimpus.shared.domain;

import java.util.Arrays;

/**
 * A rule broken by input the person can fix: a nickname that is too short, too
 * many interests, an empty message.
 *
 * <p>Rules that only a bug could break (a missing id, say) throw a plain
 * {@link IllegalArgumentException} instead: there is nothing to explain to the
 * user there.
 */
public class RuleViolationException extends IllegalArgumentException implements UserFacingError {

    private final String messageKey;
    private final Object[] messageArgs;

    public RuleViolationException(String messageKey, Object... messageArgs) {
        super(messageArgs.length == 0 ? messageKey : messageKey + " " + Arrays.toString(messageArgs));
        this.messageKey = messageKey;
        this.messageArgs = messageArgs.clone();
    }

    @Override
    public String messageKey() {
        return messageKey;
    }

    @Override
    public Object[] messageArgs() {
        return messageArgs.clone();
    }
}
