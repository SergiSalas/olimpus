package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port: who has finished signing up and can enter a round. */
public interface ProfileDirectory {

    List<Profile> everyoneWithProfile();

    Optional<Profile> byAccountId(UUID accountId);
}
