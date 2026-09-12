package com.sergisalas.olimpus.matching.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.matching.domain.Gente;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetTodaysConversationTest {

    private MundoDeRondas mundo;

    @BeforeEach
    void setUp() {
        mundo = new MundoDeRondas();
    }

    @Test
    void sin_conversacion_no_devuelve_nada_pero_tampoco_falla() {
        mundo.gente.addAll(Gente.poblacion(4, 1));

        var hoy = mundo.conversacionDeHoy().execute(mundo.gente.get(0).accountId());

        assertThat(hoy.conversation()).isEmpty();
        assertThat(hoy.partner()).isEmpty();
    }

    @Test
    void del_otro_solo_se_ve_edad_dos_intereses_y_la_distancia() {
        Profile ana =
                Gente.persona()
                        .nickname("Ana")
                        .gender(Gender.MUJER)
                        .busca(Gender.HOMBRE)
                        .edad(30)
                        .en(41.3874, 2.1686)
                        .intereses("cine", "escalada", "vinos", "teatro", "correr")
                        .build();
        Profile leo =
                Gente.persona()
                        .nickname("Leo")
                        .gender(Gender.HOMBRE)
                        .busca(Gender.MUJER)
                        .edad(33)
                        .en(41.4036, 2.1744)
                        .intereses("escalada", "cine", "podcasts", "surf", "ajedrez")
                        .build();
        mundo.gente.addAll(List.of(ana, leo));
        mundo.rondaDiaria().execute(Gente.HOY, RoundKind.PRINCIPAL);
        mundo.ahora = Instant.parse("2026-09-12T08:00:00Z");

        var hoy = mundo.conversacionDeHoy().execute(ana.accountId());

        assertThat(hoy.conversation()).isPresent();
        var vista = hoy.partner().orElseThrow();
        assertThat(vista.age()).isEqualTo(33);
        assertThat(vista.level()).isZero();
        assertThat(vista.interestsShown()).hasSize(2);
        assertThat(vista.approxDistanceKm()).isBetween(1, 4);

        // Los dos intereses que se ven son de los que tienen en comun: son los
        // que dan de que hablar.
        assertThat(hoy.sharedInterests()).contains("cine", "escalada");
        assertThat(vista.interestsShown()).allMatch(hoy.sharedInterests()::contains);
    }

    @Test
    void la_vista_del_nivel_cero_no_deja_escapar_el_apodo_ni_la_bio() {
        // Comprobacion de forma: la vista solo tiene cuatro campos, y ninguno es
        // el perfil. Si alguien añadiera el apodo aqui, este test se lo recuerda.
        var campos =
                java.util.Arrays.stream(
                                com.sergisalas.olimpus.matching.domain.PartnerView.class
                                        .getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList();

        assertThat(campos).containsExactly("age", "interestsShown", "approxDistanceKm", "level");
    }

    @Test
    void cada_uno_ve_al_otro_no_a_si_mismo() {
        Profile ana = Gente.persona().nickname("Ana").gender(Gender.MUJER).busca(Gender.HOMBRE).edad(30).build();
        Profile leo = Gente.persona().nickname("Leo").gender(Gender.HOMBRE).busca(Gender.MUJER).edad(44).build();
        mundo.gente.addAll(List.of(ana, leo));
        mundo.rondaDiaria().execute(Gente.HOY, RoundKind.PRINCIPAL);

        var deAna = mundo.conversacionDeHoy().execute(ana.accountId());
        var deLeo = mundo.conversacionDeHoy().execute(leo.accountId());

        assertThat(deAna.partner().orElseThrow().age()).isEqualTo(44);
        assertThat(deLeo.partner().orElseThrow().age()).isEqualTo(30);
    }
}
