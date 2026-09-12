package com.sergisalas.olimpus.profile.domain;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Optional<Profile> findByAccountId(UUID accountId);

    void save(Profile profile);
}
