package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.Gente.HOY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScorerTest {

    private final MatchContext ctx = MatchContext.on(HOY).build();

    @Test
    void la_pareja_vale_lo_que_vale_para_el_que_sale_peor_parado() {
        // A Ana le encaja Carlos, pero a Carlos Ana le queda fuera de rango.
        Profile ana = Gente.persona().edad(30).edades(18, 99).build();
        Profile carlos = Gente.persona().edad(30).edades(18, 22).build();

        ScoredPair pareja = Scorer.score(ana, carlos, ctx);

        assertThat(pareja.score()).isEqualTo(Math.min(pareja.sideA(), pareja.sideB()));
        assertThat(pareja.sideA()).isGreaterThan(pareja.sideB());
    }

    @Test
    void la_puntuacion_siempre_esta_entre_cero_y_uno() {
        for (Profile a : Gente.poblacion(20, 7)) {
            for (Profile b : Gente.poblacion(20, 8)) {
                assertThat(Scorer.directional(a, b, ctx)).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void la_edad_cercana_es_el_factor_que_mas_pesa() {
        Profile ana = Gente.persona().edad(30).build();
        Profile suEdad = Gente.persona().edad(31).build();
        Profile veinte_anos_mas = Gente.persona().edad(51).build();

        assertThat(Scorer.directional(ana, suEdad, ctx))
                .isGreaterThan(Scorer.directional(ana, veinte_anos_mas, ctx));

        assertThat(Scorer.ageClosenessFit(ana, suEdad, ctx)).isGreaterThan(0.8);
        assertThat(Scorer.ageClosenessFit(ana, veinte_anos_mas, ctx)).isLessThan(0.05);
    }

    @Test
    void compartir_un_interes_raro_vale_mucho_mas_que_compartir_uno_comun() {
        // Una poblacion donde "viajar" lo marca todo el mundo y "kendo" casi nadie.
        List<Profile> poblacion = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            poblacion.add(Gente.persona().intereses("viajar", "cine", "leer", "correr", "surf").build());
        }
        poblacion.add(Gente.persona().intereses("kendo", "cine", "leer", "correr", "surf").build());

        MatchContext conPesos =
                MatchContext.on(HOY)
                        .interestWeights(InterestWeights.fromPopulation(poblacion))
                        .build();

        Profile ana = Gente.persona().intereses("viajar", "kendo", "arte", "teatro", "vinos").build();
        Profile comparteComun =
                Gente.persona().intereses("viajar", "podcasts", "bailar", "surf", "ciclismo").build();
        Profile comparteRaro =
                Gente.persona().intereses("kendo", "podcasts", "bailar", "surf", "ciclismo").build();

        double comun = conPesos.interestWeights().similarity(ana.interests(), comparteComun.interests());
        double raro = conPesos.interestWeights().similarity(ana.interests(), comparteRaro.interests());

        assertThat(raro).isGreaterThan(comun * 2);
    }

    @Test
    void dos_personas_calladas_prometen_menos_que_dos_sociables() {
        Profile calladaA = Gente.persona().sociabilidad(1).profundidad(3).build();
        Profile calladaB = Gente.persona().sociabilidad(1).profundidad(3).build();
        Profile sociableA = Gente.persona().sociabilidad(5).profundidad(3).build();
        Profile sociableB = Gente.persona().sociabilidad(5).profundidad(3).build();

        assertThat(Scorer.sociabilityFit(sociableA, sociableB))
                .isGreaterThan(Scorer.sociabilityFit(calladaA, calladaB));
    }

    @Test
    void querer_la_misma_clase_de_conversacion_suma() {
        Profile hondaA = Gente.persona().sociabilidad(3).profundidad(5).build();
        Profile hondaB = Gente.persona().sociabilidad(3).profundidad(5).build();
        Profile ligera = Gente.persona().sociabilidad(3).profundidad(1).build();

        assertThat(Scorer.sociabilityFit(hondaA, hondaB))
                .isGreaterThan(Scorer.sociabilityFit(hondaA, ligera));
    }

    @Test
    void alguien_con_quien_no_has_hablado_nunca_es_novedad_total() {
        Profile ana = Gente.cualquiera();
        Profile desconocido = Gente.cualquiera();

        assertThat(Scorer.noveltyFit(ana, desconocido, ctx)).isEqualTo(1.0);
    }

    @Test
    void repetir_con_alguien_de_ayer_puntua_casi_cero_y_se_recupera_con_los_dias() {
        Profile ana = Gente.cualquiera();
        Profile carlos = Gente.cualquiera();

        MatchContext ayer =
                MatchContext.on(HOY).talked(ana.accountId(), carlos.accountId(), HOY.minusDays(1)).build();
        MatchContext haceUnMes =
                MatchContext.on(HOY).talked(ana.accountId(), carlos.accountId(), HOY.minusDays(30)).build();

        assertThat(Scorer.noveltyFit(ana, carlos, ayer)).isLessThan(0.15);
        assertThat(Scorer.noveltyFit(ana, carlos, haceUnMes)).isGreaterThan(0.9);
    }

    @Test
    void los_pesos_suman_uno() {
        assertThat(Scorer.SUMA_PESOS).isEqualTo(1.0);
    }

    @Test
    void la_tabla_de_intenciones_es_simetrica() {
        for (var a : com.sergisalas.olimpus.profile.domain.Intent.values()) {
            for (var b : com.sergisalas.olimpus.profile.domain.Intent.values()) {
                assertThat(IntentFit.between(a, b)).isEqualTo(IntentFit.between(b, a));
            }
        }
    }
}
