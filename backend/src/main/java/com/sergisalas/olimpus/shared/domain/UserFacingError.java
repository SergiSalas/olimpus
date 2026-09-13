package com.sergisalas.olimpus.shared.domain;

/**
 * An error the person using the app can understand and act on.
 *
 * <p>It carries a message key instead of finished text: the domain stays
 * language-neutral, and the HTTP adapter words it in the user's language using
 * {@code resources/messages.properties}.
 */
public interface UserFacingError {

    String messageKey();

    Object[] messageArgs();
}
