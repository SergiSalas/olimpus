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

    /** 12 de septiembre de 2026, el mismo dia que usa TestProfiles. */
    private final Clock reloj = Clock.fixed(Instant.parse("2026-09-12T08:00:00Z"), ZoneOffset.UTC);

    private Map<UUID, Profile> guardados;
    private SaveProfile guardarPerfil;
    private GetProfile leerPerfil;

    @BeforeEach
    void setUp() {
        guardados = new HashMap<>();
        ProfileRepository repo =
                new ProfileRepository() {
                    @Override
                    public Optional<Profile> findByAccountId(UUID accountId) {
                        return Optional.ofNullable(guardados.get(accountId));
                    }

                    @Override
                    public void save(Profile profile) {
                        guardados.put(profile.accountId(), profile);
                    }
                };
        guardarPerfil = new SaveProfile(repo, reloj);
        leerPerfil = new GetProfile(repo);
    }

    @Test
    void guarda_el_registro_y_se_puede_volver_a_leer() {
        guardarPerfil.execute(TestProfiles.valido().build());

        assertThat(leerPerfil.execute(TestProfiles.CUENTA))
                .get()
                .extracting(Profile::nickname)
                .isEqualTo("Sergi");
    }

    @Test
    void volver_a_guardar_sustituye_el_anterior() {
        guardarPerfil.execute(TestProfiles.valido().build());
        guardarPerfil.execute(TestProfiles.valido().nickname("Sergio").build());

        assertThat(guardados).hasSize(1);
        assertThat(leerPerfil.execute(TestProfiles.CUENTA).orElseThrow().nickname())
                .isEqualTo("Sergio");
    }

    @Test
    void un_menor_de_edad_no_entra_aunque_el_movil_diga_otra_cosa() {
        Profile menor = TestProfiles.valido().birthDate(TestProfiles.HOY.minusYears(17)).build();

        assertThatThrownBy(() -> guardarPerfil.execute(menor))
                .isInstanceOf(UnderageException.class)
                .hasMessageContaining("mayores de 18");

        assertThat(guardados).isEmpty();
    }

    @Test
    void quien_no_ha_hecho_el_registro_no_tiene_perfil() {
        assertThat(leerPerfil.execute(UUID.randomUUID())).isEmpty();
    }
}
