package com.sergisalas.olimpus.profile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void la_ubicacion_se_guarda_redondeada_a_dos_decimales() {
        Location exacta = Location.rounded(41.387423, 2.168665);

        assertThat(exacta.latitude()).isEqualTo(41.39);
        assertThat(exacta.longitude()).isEqualTo(2.17);
    }

    @Test
    void el_redondeo_pierde_como_mucho_algo_mas_de_un_kilometro() {
        Location exacta = new Location(41.387423, 2.168665);
        Location guardada = Location.rounded(41.387423, 2.168665);

        assertThat(exacta.distanceKmTo(guardada)).isLessThan(1.6);
    }

    @Test
    void dos_puntos_de_la_misma_ciudad_estan_a_pocos_kilometros() {
        Location sagradaFamilia = Location.rounded(41.4036, 2.1744);
        Location barceloneta = Location.rounded(41.3797, 2.1900);

        assertThat(sagradaFamilia.distanceKmTo(barceloneta)).isBetween(2.0, 4.0);
    }

    @Test
    void barcelona_y_madrid_estan_a_unos_seiscientos_kilometros() {
        Location barcelona = Location.rounded(41.3874, 2.1686);
        Location madrid = Location.rounded(40.4168, -3.7038);

        assertThat(barcelona.distanceKmTo(madrid)).isBetween(500.0, 520.0);
    }

    @Test
    void la_distancia_de_un_punto_a_si_mismo_es_cero() {
        Location aqui = Location.rounded(41.3874, 2.1686);

        assertThat(aqui.distanceKmTo(aqui)).isZero();
    }

    @Test
    void unas_coordenadas_imposibles_se_rechazan() {
        assertThatThrownBy(() -> new Location(95, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Location(0, 200)).isInstanceOf(IllegalArgumentException.class);
    }
}
