package com.sergisalas.olimpus.auth.domain;

/**
 * Port: get the code to its owner.
 *
 * <p>It is one of the swappable pieces of the project. While developing, the
 * code is written to the console; once there is budget, a real email service is
 * plugged in without touching anything else.
 */
public interface CodeSender {

    void send(EmailAddress email, String code);
}
