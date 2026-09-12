package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.Gente.HOY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.LanguageSkill;
import com.sergisalas.olimpus.profile.domain.Profile;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FiltersTest {

    private final MatchContext ctx = MatchContext.on(HOY).build();

    @Nested
    class LosQueNuncaCeden {

        @Test
        void nadie_habla_consigo_mismo() {
            Profile ana = Gente.cualquiera();

            assertThat(Filters.passesHard(ana, ana, ctx)).isFalse();
        }

        @Test
        void el_genero_tiene_que_encajar_por_los_dos_lados() {
            Profile ana = Gente.persona().gender(Gender.MUJER).busca(Gender.HOMBRE).build();
            Profile carlos = Gente.persona().gender(Gender.HOMBRE).busca(Gender.MUJER).build();
            Profile luis = Gente.persona().gender(Gender.HOMBRE).busca(Gender.HOMBRE).build();

            assertThat(Filters.passesHard(ana, carlos, ctx)).isTrue();
            // A Ana le encaja Luis, pero Luis no busca mujeres: no hay pareja.
            assertThat(Filters.passesHard(ana, luis, ctx)).isFalse();
            assertThat(Filters.passesHard(luis, ana, ctx)).isFalse();
        }

        @Test
        void un_bloqueo_corta_en_las_dos_direcciones() {
            Profile ana = Gente.cualquiera();
            Profile carlos = Gente.cualquiera();
            MatchContext conBloqueo =
                    MatchContext.on(HOY).blocked(ana.accountId(), carlos.accountId()).build();

            assertThat(Filters.passesHard(ana, carlos, conBloqueo)).isFalse();
            assertThat(Filters.passesHard(carlos, ana, conBloqueo)).isFalse();
        }

        @Test
        void por_mucho_que_se_espere_el_nucleo_duro_no_se_mueve() {
            Profile ana = Gente.persona().gender(Gender.MUJER).busca(Gender.HOMBRE).build();
            Profile luis = Gente.persona().gender(Gender.HOMBRE).busca(Gender.HOMBRE).build();
            MatchContext esperandoSiglos =
                    MatchContext.on(HOY).waiting(ana.accountId(), 500).build();

            assertThat(Filters.passesHard(ana, luis, esperandoSiglos)).isFalse();
        }
    }

    @Nested
    class LosQueCedenConLaEspera {

        @Test
        void sin_espera_las_preferencias_se_respetan_tal_cual() {
            Profile ana = Gente.persona().distancia(5).en(41.3874, 2.1686).build();
            Profile lejos = Gente.persona().distancia(50).en(41.4500, 2.1686).build();

            assertThat(Filters.passesSoft(ana, lejos, ctx, 0)).isFalse();
        }

        @Test
        void con_la_maxima_relajacion_el_radio_llega_al_triple() {
            Profile ana = Gente.persona().distancia(5).en(41.3874, 2.1686).build();
            // Unos 7 km al norte: fuera de 5 km, dentro de 15.
            Profile aSieteKm = Gente.persona().distancia(50).en(41.4500, 2.1686).build();

            assertThat(Filters.passesSoft(ana, aSieteKm, ctx, 1)).isTrue();
        }

        @Test
        void la_distancia_tambien_es_mutua() {
            Profile ana = Gente.persona().distancia(50).en(41.3874, 2.1686).build();
            Profile casero = Gente.persona().distancia(5).en(41.4500, 2.1686).build();

            assertThat(Filters.passesSoft(ana, casero, ctx, 0)).isFalse();
        }

        @Test
        void el_rango_de_edad_se_ensancha_hasta_cinco_anos_por_lado() {
            Profile ana = Gente.persona().edad(30).edades(28, 35).build();
            Profile joven = Gente.persona().edad(25).edades(18, 99).build();

            assertThat(Filters.passesSoft(ana, joven, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(ana, joven, ctx, 1)).isTrue();
        }

        @Test
        void sin_idioma_en_comun_no_hay_conversacion_ni_esperando_mucho() {
            Profile ana = Gente.persona().idiomas(new LanguageSkill("es", LanguageSkill.Level.NATIVO)).build();
            Profile jan = Gente.persona().idiomas(new LanguageSkill("de", LanguageSkill.Level.NATIVO)).build();

            assertThat(Filters.passesSoft(ana, jan, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(ana, jan, ctx, 1)).isFalse();
        }

        @Test
        void un_idioma_flojo_en_comun_solo_vale_despues_de_esperar() {
            Profile ana =
                    Gente.persona()
                            .idiomas(
                                    new LanguageSkill("es", LanguageSkill.Level.NATIVO),
                                    new LanguageSkill("en", LanguageSkill.Level.BASICO))
                            .build();
            Profile john =
                    Gente.persona().idiomas(new LanguageSkill("en", LanguageSkill.Level.NATIVO)).build();

            assertThat(Filters.passesSoft(ana, john, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(ana, john, ctx, 1)).isTrue();
        }

        @Test
        void intenciones_muy_distintas_solo_se_cruzan_al_final() {
            Profile busca_pareja = Gente.persona().busca(Intent.PAREJA).build();
            Profile busca_casual = Gente.persona().busca(Intent.CASUAL).build();

            assertThat(Filters.passesSoft(busca_pareja, busca_casual, ctx, 0)).isFalse();
            assertThat(Filters.passesSoft(busca_pareja, busca_casual, ctx, 1)).isTrue();
        }
    }

    @Nested
    class LaRelajacion {

        @Test
        void crece_con_los_dias_y_se_para_a_los_tres() {
            assertThat(Filters.relaxationForDaysWaiting(0)).isZero();
            assertThat(Filters.relaxationForDaysWaiting(1)).isCloseTo(0.33, org.assertj.core.data.Offset.offset(0.01));
            assertThat(Filters.relaxationForDaysWaiting(3)).isEqualTo(1.0);
            assertThat(Filters.relaxationForDaysWaiting(40)).isEqualTo(1.0);
        }

        @Test
        void manda_el_que_mas_ha_esperado_de_los_dos() {
            Profile ana = Gente.cualquiera();
            Profile carlos = Gente.cualquiera();
            MatchContext unoDesesperado =
                    MatchContext.on(HOY)
                            .waiting(ana.accountId(), 0)
                            .waiting(carlos.accountId(), 3)
                            .build();

            assertThat(Filters.relaxationFor(ana, carlos, unoDesesperado)).isEqualTo(1.0);
        }

        @Test
        void el_nivel_de_idioma_exigido_baja_pero_tiene_suelo() {
            assertThat(Filters.requiredLanguageLevel(0)).isEqualTo(0.70);
            assertThat(Filters.requiredLanguageLevel(1)).isEqualTo(0.35);
            assertThat(Filters.requiredLanguageLevel(5)).isEqualTo(0.35);
        }
    }
}
