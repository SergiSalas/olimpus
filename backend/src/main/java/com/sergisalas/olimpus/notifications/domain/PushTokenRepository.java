package com.sergisalas.olimpus.notifications.domain;

import java.util.List;
import java.util.UUID;

/** Where each person's phones are reachable. Someone may have more than one. */
public interface PushTokenRepository {

    void save(UUID accountId, String token);

    List<String> tokensOf(UUID accountId);

    /** A token the push service says is dead: it is dropped, not retried forever. */
    void delete(String token);
}
