package com.sergisalas.olimpus.shared.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256HasherTest {

    private final Sha256Hasher hasher = new Sha256Hasher();

    @Test
    void la_huella_no_deja_ver_el_secreto() {
        assertThat(hasher.hash("246810")).doesNotContain("246810");
    }

    @Test
    void el_mismo_secreto_da_siempre_la_misma_huella() {
        assertThat(hasher.hash("246810")).isEqualTo(hasher.hash("246810"));
    }

    @Test
    void dos_secretos_parecidos_dan_huellas_distintas() {
        assertThat(hasher.hash("246810")).isNotEqualTo(hasher.hash("246811"));
    }

    @Test
    void la_huella_es_texto_corto_y_apto_para_guardar_en_la_base_de_datos() {
        assertThat(hasher.hash("una-llave-de-sesion-larguisima"))
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
    }
}
