package com.sergisalas.olimpus.auth.domain;

import java.util.Optional;

/** Solo hay un codigo vivo por email: pedir uno nuevo sustituye al anterior. */
public interface LoginCodeRepository {

    Optional<LoginCode> findByEmail(EmailAddress email);

    void save(LoginCode code);

    void deleteByEmail(EmailAddress email);
}
