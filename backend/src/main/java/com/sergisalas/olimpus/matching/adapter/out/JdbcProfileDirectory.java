package com.sergisalas.olimpus.matching.adapter.out;

import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.ProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Matching needs everyone at once; the profile module only looks people up one
 * by one. This adapter provides the full list, reusing the profile repository to
 * read each profile with its languages.
 */
@Repository
public class JdbcProfileDirectory implements ProfileDirectory {

    private final JdbcTemplate jdbc;
    private final ProfileRepository profiles;

    public JdbcProfileDirectory(JdbcTemplate jdbc, ProfileRepository profiles) {
        this.jdbc = jdbc;
        this.profiles = profiles;
    }

    @Override
    public List<Profile> everyoneWithProfile() {
        List<UUID> ids =
                jdbc.queryForList("select account_id from profile order by created_at", UUID.class);
        return ids.stream().map(profiles::findByAccountId).flatMap(Optional::stream).toList();
    }

    @Override
    public Optional<Profile> byAccountId(UUID accountId) {
        return profiles.findByAccountId(accountId);
    }
}
