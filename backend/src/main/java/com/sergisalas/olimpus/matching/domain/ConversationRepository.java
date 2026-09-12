package com.sergisalas.olimpus.matching.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    void save(Conversation conversation);

    List<Conversation> byDate(LocalDate date);

    /** La conversacion viva de alguien hoy, si tiene alguna. */
    Optional<Conversation> openFor(UUID accountId, LocalDate date);

    Optional<Conversation> byId(UUID id);
}
