package com.sergisalas.olimpus.auth.domain;

import java.util.Optional;

/** There is only one live code per email: requesting a new one replaces the previous one. */
public interface LoginCodeRepository {

    Optional<LoginCode> findByEmail(EmailAddress email);

    void save(LoginCode code);

    void deleteByEmail(EmailAddress email);
}
