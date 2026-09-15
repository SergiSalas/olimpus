package com.sergisalas.olimpus.matching.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    void save(Conversation conversation);

    List<Conversation> byDate(LocalDate date);

    /** Someone's live conversation on that day, if they have one. */
    Optional<Conversation> openFor(UUID accountId, LocalDate date);

    Optional<Conversation> byId(UUID id);

    /** The conversations that ended in a mutual yes. They never expire. */
    List<Conversation> connectionsOf(UUID accountId);
}
