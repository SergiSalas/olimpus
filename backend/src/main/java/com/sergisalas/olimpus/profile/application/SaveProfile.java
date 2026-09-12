package com.sergisalas.olimpus.profile.application;

import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.profile.domain.UnderageException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Caso de uso: guardar el registro. Sirve para crearlo y para cambiarlo.
 *
 * <p>La edad se calcula con el reloj del servidor, no con lo que diga el
 * movil. Mas adelante habra tambien verificacion de edad real; esto es solo la
 * primera barrera.
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
