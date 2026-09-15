package com.sergisalas.olimpus.shared.domain;

/**
 * An error the person using the app can understand and act on.
 *
 * <p>It carries a message key instead of finished text: the domain stays
 * language-neutral, and the HTTP adapter words it in the user's language using
 * {@code resources/messages.properties}.
 *
 * <p>It also carries how serious it is, not as an HTTP code (the domain knows
 * nothing about HTTP) but as a kind the adapter translates.
 */
public interface UserFacingError {

    /** How the outside world should take this error. */
    enum Kind {
        /** Something in the request the person can fix. */
        INVALID_INPUT,
        /** No valid session. */
        NOT_AUTHENTICATED,
        /** A rule about who may do what: age, for instance. */
        NOT_ALLOWED,
        /** Nothing there, or nothing this person may see. */
        NOT_FOUND,
        /** The moment is wrong: too soon, already closed, already over. */
        WRONG_MOMENT
    }

    String messageKey();

    Object[] messageArgs();

    /**
     * Most user errors are bad input; the rest say so. Having a default means a
     * new error answers sensibly the moment it is written, instead of turning
     * into a 500 because someone forgot to register it.
     */
    default Kind kind() {
        return Kind.INVALID_INPUT;
    }
}
