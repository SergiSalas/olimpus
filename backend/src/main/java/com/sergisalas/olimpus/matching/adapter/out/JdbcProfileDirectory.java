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
 * El reparto necesita a todo el mundo de golpe; el registro solo sabe buscar de
 * uno en uno. Este adaptador pone la lista completa, reutilizando el repositorio
 * del registro para leer cada perfil con sus idiomas.
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
