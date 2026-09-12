package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.Gente.HOY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * No comprueba reglas nuevas: imprime como queda una ronda para poder mirarla
 * con ojos humanos. Los numeros que salen aqui son los que habra que comparar
 * con los de la app real cuando haya datos de verdad.
 */
class RoundDemo {

    @Test
    void resumen_de_una_ronda_de_doscientas_personas() {
        List<Profile> gente = Gente.poblacion(200, 2026);
        MatchContext ctx =
                MatchContext.on(HOY).interestWeights(InterestWeights.fromPopulation(gente)).build();

        List<Match> ronda = DailyRound.plan(gente, ctx, new Random(2026));
        List<java.util.UUID> sinPareja = DailyRound.leftOut(gente, ronda);

        System.out.printf("%n=== RONDA DEL %s ===%n", HOY);
        System.out.printf("Personas en el reparto......... %d%n", gente.size());
        System.out.printf("Parejas formadas.............. %d%n", ronda.size());
        System.out.printf(
                "Se quedan sin conversacion.... %d (%.0f%%)%n",
                sinPareja.size(), 100.0 * sinPareja.size() / gente.size());

        System.out.printf("%n%-16s %8s %10s%n", "DE DONDE SALE", "PAREJAS", "PROMESA");
        for (Origin origin : Origin.values()) {
            List<Match> deEseTipo = ronda.stream().filter(m -> m.origin() == origin).toList();
            double media =
                    deEseTipo.stream().mapToDouble(Match::score).average().orElse(0);
            System.out.printf("%-16s %8d %9.2f%n", origin, deEseTipo.size(), media);
        }

        double elegidas =
                ronda.stream()
                        .filter(m -> m.origin() == Origin.MEJOR_PAREJA)
                        .mapToDouble(Match::score)
                        .average()
                        .orElse(0);
        double azar =
                ronda.stream()
                        .filter(m -> m.origin() == Origin.AZAR)
                        .mapToDouble(Match::score)
                        .average()
                        .orElse(0);

        System.out.printf(
                "%nLas elegidas prometen un %.0f%% mas que las del azar.%n",
                100 * (elegidas / azar - 1));
        System.out.println(
                "Ojo: eso es lo que promete el algoritmo, no lo que pasa de verdad.");
        System.out.println(
                "Quien tenia razon se sabra con los datos de la beta, comparando");
        System.out.println("cuantas de cada tipo siguen hablando 48 horas despues.");
        System.out.println();

        assertThat(ronda).isNotEmpty();
    }
}
