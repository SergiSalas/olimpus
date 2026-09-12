package com.sergisalas.olimpus.profile.domain;

import static com.sergisalas.olimpus.profile.domain.TestProfiles.HOY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileTest {

    @Test
    void un_perfil_completo_se_construye_sin_quejas() {
        assertThat(TestProfiles.valido().build().nickname()).isEqualTo("Sergi");
    }

    @Test
    void la_edad_sale_de_la_fecha_de_nacimiento() {
        Profile sergi = TestProfiles.valido().birthDate(LocalDate.of(1995, 3, 20)).build();

        assertThat(sergi.ageOn(HOY)).isEqualTo(31);
        assertThat(sergi.isMinorOn(HOY)).isFalse();
    }

    @Test
    void el_dia_antes_de_cumplir_dieciocho_todavia_es_menor() {
        Profile casi = TestProfiles.valido().birthDate(HOY.minusYears(18).plusDays(1)).build();
        Profile justo = TestProfiles.valido().birthDate(HOY.minusYears(18)).build();

        assertThat(casi.isMinorOn(HOY)).isTrue();
        assertThat(justo.isMinorOn(HOY)).isFalse();
    }

    @Test
    void las_escalas_de_uno_a_cinco_se_traducen_a_cero_y_uno_para_el_algoritmo() {
        assertThat(TestProfiles.valido().sociability(1).build().sociabilityScore()).isZero();
        assertThat(TestProfiles.valido().sociability(3).build().sociabilityScore()).isEqualTo(0.5);
        assertThat(TestProfiles.valido().sociability(5).build().sociabilityScore()).isEqualTo(1.0);
        assertThat(TestProfiles.valido().conversationDepth(5).build().conversationDepthScore())
                .isEqualTo(1.0);
    }

    @Test
    void el_apodo_se_limpia_de_espacios() {
        assertThat(TestProfiles.valido().nickname("  Ana  ").build().nickname()).isEqualTo("Ana");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A", "unapodoexageradamentelargodeverdad"})
    void un_apodo_demasiado_corto_o_largo_no_vale(String apodo) {
        assertThatThrownBy(() -> TestProfiles.valido().nickname(apodo).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apodo");
    }

    @Test
    void la_bio_tiene_tope() {
        assertThatThrownBy(() -> TestProfiles.valido().bio("x".repeat(201)).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bio");
    }

    @Test
    void hay_que_buscar_al_menos_un_genero() {
        assertThatThrownBy(() -> TestProfiles.valido().seeking(Set.of()).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("genero");
    }

    @Test
    void el_rango_de_edad_no_puede_estar_al_reves() {
        assertThatThrownBy(() -> TestProfiles.valido().ages(40, 25).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al reves");
    }

    @Test
    void nadie_puede_buscar_menores() {
        assertThatThrownBy(() -> TestProfiles.valido().ages(16, 30).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rango de edad");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 20, 100})
    void la_distancia_solo_puede_ser_una_de_las_cuatro(int km) {
        assertThatThrownBy(() -> TestProfiles.valido().maxDistanceKm(km).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distancia");
    }

    @Test
    void hacen_falta_entre_cinco_y_ocho_intereses() {
        assertThatThrownBy(() -> TestProfiles.valido().interests(Set.of("cine", "leer")).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intereses");

        assertThatThrownBy(
                        () ->
                                TestProfiles.valido()
                                        .interests(
                                                Set.of(
                                                        "cine", "leer", "correr", "surf", "vinos",
                                                        "teatro", "arte", "buceo", "kendo"))
                                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intereses");
    }

    @Test
    void un_interes_inventado_no_vale() {
        assertThatThrownBy(
                        () ->
                                TestProfiles.valido()
                                        .interests(
                                                Set.of("cine", "leer", "correr", "surf", "puenting-lunar"))
                                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no esta en la lista");
    }

    @Test
    void hace_falta_al_menos_un_idioma_y_sin_repetir() {
        assertThatThrownBy(() -> TestProfiles.valido().languages(List.of()).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idioma");

        assertThatThrownBy(
                        () ->
                                TestProfiles.valido()
                                        .languages(
                                                List.of(
                                                        new LanguageSkill("es", LanguageSkill.Level.NATIVO),
                                                        new LanguageSkill("es", LanguageSkill.Level.BASICO)))
                                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    void el_catalogo_de_intereses_es_el_del_laboratorio() {
        assertThat(InterestCatalog.size()).isEqualTo(38);
        assertThat(InterestCatalog.contains("escalada-en-hielo")).isTrue();
        assertThat(InterestCatalog.contains("viajar")).isTrue();
        assertThat(InterestCatalog.contains("lo-que-sea")).isFalse();
    }
}
