package com.sergisalas.olimpus.profile.application;

import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.profile.domain.UnderageException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Use case: save the sign-up. It serves both to create it and to change it.
 *
 * <p>Age is computed with the server clock, not with whatever the phone says.
 * Real age verification will come later; this is only the first barrier.
 */
public class SaveProfile {

    private final ProfileRepository profiles;
    private final Clock clock;

    public SaveProfile(ProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    public Profile execute(Profile profile) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (profile.isMinorOn(today)) {
            throw new UnderageException();
        }
        profiles.save(profile);
        return profile;
    }
}
