package com.sergisalas.olimpus.profile.application;

import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import java.util.Optional;
import java.util.UUID;

public class GetProfile {

    private final ProfileRepository profiles;

    public GetProfile(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    public Optional<Profile> execute(UUID accountId) {
        return profiles.findByAccountId(accountId);
    }
}
