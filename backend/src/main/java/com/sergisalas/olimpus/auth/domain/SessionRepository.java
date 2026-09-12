package com.sergisalas.olimpus.auth.domain;

import java.util.Optional;

public interface SessionRepository {

    Optional<Session> findByTokenHash(String tokenHash);

    void save(Session session);
}
