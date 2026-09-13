package com.sergisalas.olimpus.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import com.sergisalas.olimpus.profile.domain.TestProfiles;
import com.sergisalas.olimpus.profile.domain.UnderageException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaveProfileTest {

    /** 12 September 2026, the same day TestProfiles uses. */
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T08:00:00Z"), ZoneOffset.UTC);

    private Map<UUID, Profile> stored;
    private SaveProfile saveProfile;
    private GetProfile getProfile;

    @BeforeEach
    void setUp() {
        stored = new HashMap<>();
        ProfileRepository repo =
                new ProfileRepository() {
                    @Override
                    public Optional<Profile> findByAccountId(UUID accountId) {
                        return Optional.ofNullable(stored.get(accountId));
                    }

                    @Override
                    public void save(Profile profile) {
                        stored.put(profile.accountId(), profile);
                    }
                };
        saveProfile = new SaveProfile(repo, clock);
        getProfile = new GetProfile(repo);
    }

    @Test
    void saves_the_sign_up_and_it_can_be_read_back() {
        saveProfile.execute(TestProfiles.valid().build());

        assertThat(getProfile.execute(TestProfiles.ACCOUNT))
                .get()
                .extracting(Profile::nickname)
                .isEqualTo("Sergi");
    }

    @Test
    void saving_again_replaces_the_previous_one() {
        saveProfile.execute(TestProfiles.valid().build());
        saveProfile.execute(TestProfiles.valid().nickname("Sergio").build());

        assertThat(stored).hasSize(1);
        assertThat(getProfile.execute(TestProfiles.ACCOUNT).orElseThrow().nickname())
                .isEqualTo("Sergio");
    }

    @Test
    void a_minor_does_not_get_in_whatever_the_phone_says() {
        Profile minor = TestProfiles.valid().birthDate(TestProfiles.TODAY.minusYears(17)).build();

        assertThatThrownBy(() -> saveProfile.execute(minor))
                .isInstanceOfSatisfying(
                        UnderageException.class,
                        e -> assertThat(e.messageKey()).isEqualTo("error.profile.underage"));

        assertThat(stored).isEmpty();
    }

    @Test
    void someone_who_has_not_signed_up_has_no_profile() {
        assertThat(getProfile.execute(UUID.randomUUID())).isEmpty();
    }
}
