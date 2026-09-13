package com.sergisalas.olimpus.auth.domain;

/** Port: generate things that cannot be guessed. */
public interface Secrets {

    /** Six-digit code, zero-padded when needed. */
    String sixDigitCode();

    /** Long session token, the one the phone keeps. */
    String sessionToken();
}
