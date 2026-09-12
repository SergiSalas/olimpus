package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto: quien tiene el registro hecho y puede entrar en un reparto. */
public interface ProfileDirectory {

    List<Profile> everyoneWithProfile();

    Optional<Profile> byAccountId(UUID accountId);
}
