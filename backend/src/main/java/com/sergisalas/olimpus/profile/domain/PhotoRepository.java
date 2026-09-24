package com.sergisalas.olimpus.profile.domain;

import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository {

    Optional<Photo> findByAccountId(UUID accountId);

    void save(Photo photo);
}
