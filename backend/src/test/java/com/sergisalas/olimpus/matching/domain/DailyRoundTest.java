package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.Gente.HOY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DailyRoundTest {

    private final MatchContext ctx = MatchContext.on(HOY).build();

    @Test
    void con_una_sola_persona_no_hay_ronda() {
        assertThat(DailyRound.plan(List.of(Gente.cualquiera()), ctx, new Random(1))).isEmpty();
        assertThat(DailyRound.plan(List.of(), ctx, new Random(1))).isEmpty();
    }

    @Test
    void dos_personas_compatibles_acaban_juntas() {
        Profile ana = Gente.persona().nickname("Ana").build();
        Profile carlos = Gente.persona().nickname("Carlos").build();

        List<Match> ronda = DailyRound.plan(List.of(ana, carlos), ctx, new Random(1));

        assertThat(ronda).hasSize(1);
        assertThat(ronda.get(0).involves(ana.accountId())).isTrue();
        assertThat(ronda.get(0).involves(carlos.accountId())).isTrue();
    }

    @Test
    void nadie_sale_en_dos_parejas_el_mismo_dia() {
        List<Profile> gente = Gente.poblacion(60, 42);

        List<Match> ronda = DailyRound.plan(gente, ctx, new Random(42));

        Set<UUID> vistos = new HashSet<>();
        for (Match match : ronda) {
            assertThat(vistos.add(match.accountA())).as("A repetido").isTrue();
            assertThat(vistos.add(match.accountB())).as("B repetido").isTrue();
        }
    }

    @Test
    void toda_pareja_de_la_ronda_pasa_los_filtros_duros_incluidas_las_del_azar() {
        List<Profile> gente = Gente.poblacion(80, 7);
        MatchContext conBloqueos =
                MatchContext.on(HOY)
                        .blocked(gente.get(0).accountId(), gente.get(1).accountId())
                        .blocked(gente.get(2).accountId(), gente.get(3).accountId())
                        .build();

        List<Match> ronda = DailyRound.plan(gente, conBloqueos, new Random(7));

        assertThat(ronda).isNotEmpty();
        for (Match match : ronda) {
            Profile a = buscar(gente, match.accountA());
            Profile b = buscar(gente, match.accountB());
            assertThat(Filters.passesHard(a, b, conBloqueos))
                    .as("pareja de origen %s", match.origin())
                    .isTrue();
        }
    }

    @Test
    void quien_esta_bloqueado_nunca_aparece_junto() {
        List<Profile> gente = Gente.poblacion(40, 3);
        UUID uno = gente.get(0).accountId();
        UUID otro = gente.get(1).accountId();
        MatchContext conBloqueo = MatchContext.on(HOY).blocked(uno, otro).build();

        List<Match> ronda = DailyRound.plan(gente, conBloqueo, new Random(3));

        assertThat(ronda)
                .noneMatch(match -> match.involves(uno) && match.involves(otro));
    }

    @Test
    void una_parte_del_reparto_es_azar_y_otra_descubrimiento() {
        List<Profile> gente = Gente.poblacion(100, 11);

        List<Match> ronda = DailyRound.plan(gente, ctx, new Random(11));

        long azar = ronda.stream().filter(m -> m.origin() == Origin.AZAR).count();
        long descubrimiento = ronda.stream().filter(m -> m.origin() == Origin.DESCUBRIMIENTO).count();
        long mejor = ronda.stream().filter(m -> m.origin() == Origin.MEJOR_PAREJA).count();

        // Con 100 personas salen unas 50 parejas, y se busca que el 10% de ellas
        // sea de azar y otro 10% de descubrimiento: unas 5 de cada.
        long total = ronda.size();
        assertThat(azar).isBetween(3L, 8L);
        assertThat(descubrimiento).isBetween(3L, 8L);
        assertThat(azar / (double) total).isBetween(0.05, 0.16);
        assertThat(descubrimiento / (double) total).isBetween(0.05, 0.16);
        assertThat(mejor).isGreaterThan(azar + descubrimiento);
    }

    @Test
    void el_reparto_es_repetible_con_la_misma_semilla() {
        List<Profile> gente = Gente.poblacion(50, 5);

        List<Match> primera = DailyRound.plan(gente, ctx, new Random(99));
        List<Match> segunda = DailyRound.plan(gente, ctx, new Random(99));
        List<Match> conOtraSemilla = DailyRound.plan(gente, ctx, new Random(100));

        assertThat(primera).isEqualTo(segunda);
        assertThat(primera).isNotEqualTo(conOtraSemilla);
    }

    @Test
    void las_parejas_elegidas_prometen_mas_que_las_del_azar() {
        List<Profile> gente = Gente.poblacion(200, 21);
        MatchContext conPesos =
                MatchContext.on(HOY).interestWeights(InterestWeights.fromPopulation(gente)).build();

        List<Match> ronda = DailyRound.plan(gente, conPesos, new Random(21));

        double mediaElegidas = media(ronda, Origin.MEJOR_PAREJA);
        double mediaAzar = media(ronda, Origin.AZAR);

        // Si esto fallara, el reparto no estaria usando la puntuacion para nada.
        assertThat(mediaElegidas).isGreaterThan(mediaAzar);
    }

    @Test
    void el_descubrimiento_es_peor_que_la_mejor_pareja_pero_mejor_que_el_azar() {
        List<Profile> gente = Gente.poblacion(200, 33);
        MatchContext conPesos =
                MatchContext.on(HOY).interestWeights(InterestWeights.fromPopulation(gente)).build();

        List<Match> ronda = DailyRound.plan(gente, conPesos, new Random(33));

        // Si descubrir diera lo mismo que el azar, habria dos partes del reparto
        // midiendo lo mismo y una de las dos no serviria para nada.
        assertThat(media(ronda, Origin.DESCUBRIMIENTO)).isLessThan(media(ronda, Origin.MEJOR_PAREJA));
        assertThat(media(ronda, Origin.DESCUBRIMIENTO)).isGreaterThan(media(ronda, Origin.AZAR));
    }

    @Test
    void quien_no_encaja_con_nadie_se_queda_fuera_y_aparece_en_la_lista_de_repesca() {
        Profile ana = Gente.persona().gender(Gender.MUJER).busca(Gender.HOMBRE).build();
        Profile eva = Gente.persona().gender(Gender.MUJER).busca(Gender.HOMBRE).build();
        Profile carlos = Gente.persona().gender(Gender.HOMBRE).busca(Gender.MUJER).build();

        List<Profile> gente = List.of(ana, eva, carlos);
        List<Match> ronda = DailyRound.plan(gente, ctx, new Random(1));

        assertThat(ronda).hasSize(1);
        assertThat(DailyRound.leftOut(gente, ronda)).hasSize(1);
    }

    @Test
    void con_mucha_gente_compatible_casi_nadie_se_queda_sin_conversacion() {
        List<Profile> gente = Gente.poblacion(100, 77);

        List<Match> ronda = DailyRound.plan(gente, ctx, new Random(77));

        assertThat(DailyRound.leftOut(gente, ronda).size()).isLessThan(15);
    }

    @Test
    void una_ronda_de_cuatrocientas_personas_se_reparte_en_menos_de_un_segundo() {
        List<Profile> gente = Gente.poblacion(400, 4);
        MatchContext conPesos =
                MatchContext.on(HOY).interestWeights(InterestWeights.fromPopulation(gente)).build();

        long antes = System.currentTimeMillis();
        List<Match> ronda = DailyRound.plan(gente, conPesos, new Random(4));
        long tardo = System.currentTimeMillis() - antes;

        assertThat(ronda).isNotEmpty();
        assertThat(tardo).as("ha tardado %d ms", tardo).isLessThan(1000);
    }

    private static double media(List<Match> ronda, Origin origin) {
        List<Double> valores = new ArrayList<>();
        for (Match match : ronda) {
            if (match.origin() == origin) valores.add(match.score());
        }
        assertThat(valores).as("no hay parejas de tipo %s", origin).isNotEmpty();
        return valores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static Profile buscar(List<Profile> gente, UUID id) {
        return gente.stream()
                .filter(p -> p.accountId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("id que no esta en el pool"));
    }
}
