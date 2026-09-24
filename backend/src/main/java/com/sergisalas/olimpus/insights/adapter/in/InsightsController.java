package com.sergisalas.olimpus.insights.adapter.in;

import com.sergisalas.olimpus.insights.application.MeasureOutcomes;
import com.sergisalas.olimpus.insights.domain.Insights;
import com.sergisalas.olimpus.matching.domain.Origin;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * How the app is doing.
 *
 * <p>Behind the same switch as the round you can trigger by hand, because there
 * is no such thing as an administrator yet. Before the beta this needs either a
 * real admin login or to be turned off: these numbers are about real people.
 */
@RestController
@RequestMapping("/api/dev/insights")
@ConditionalOnProperty(name = "olimpus.dev-endpoints", havingValue = "true")
public class InsightsController {

    private final MeasureOutcomes measureOutcomes;

    public InsightsController(MeasureOutcomes measureOutcomes) {
        this.measureOutcomes = measureOutcomes;
    }

    @GetMapping
    public Insights json(@RequestParam(defaultValue = "14") int days) {
        return measureOutcomes.lastDays(days);
    }

    /** The same thing, written out, because numbers alone do not say what they mean. */
    @GetMapping(value = "/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public String text(@RequestParam(defaultValue = "14") int days) {
        Insights insights = measureOutcomes.lastDays(days);
        StringBuilder out = new StringBuilder();

        out.append(String.format("%n=== OLIMPUS · %s a %s ===%n%n", insights.from(), insights.to()));
        out.append(
                String.format(
                        "%-16s %7s %9s %9s %11s%n",
                        "DE DONDE SALE", "PAREJAS", "ARRANCAN", "CONECTAN", "A LAS 48 H"));

        for (Insights.ByOrigin row : insights.byOrigin()) {
            out.append(
                    String.format(
                            Locale.ROOT,
                            "%-16s %7d %8.0f%% %8.0f%% %10.0f%%%n",
                            spanish(row.origin()),
                            row.pairs(),
                            row.tookOffRate() * 100,
                            row.connectionRate() * 100,
                            row.survivalRate() * 100));
        }

        out.append(String.format("%n%s%n", verdict(insights)));

        Insights.Alarms alarms = insights.alarms();
        out.append(String.format("%nLAS ALARMAS%n"));
        out.append(String.format("  Reportes................... %d%n", alarms.reports()));
        out.append(String.format("  Bloqueos................... %d%n", alarms.blocks()));
        out.append(
                String.format(
                        "  Una semana sin nadie....... %d personas%n",
                        alarms.peopleWithNoMatchInAWeek()));
        out.append(
                String.format(
                        Locale.ROOT, "  Espera media............... %.1f días%n", alarms.averageDaysWaiting()));
        out.append(
                String.format("  Conversaciones en silencio. %d%n", alarms.diedInSilence()));
        out.append(
                String.format(
                        Locale.ROOT,
                        "  El 10%% más activo se lleva. %.0f%% (lo justo seria %.0f%%)%n",
                        alarms.shareTakenByTopTenth() * 100,
                        alarms.evenShareWouldBe() * 100));

        return out.toString();
    }

    /**
     * The sentence the whole thing exists for. It refuses to claim anything when
     * there is not enough data, because "+32%" out of ten pairs means nothing.
     */
    private static String verdict(Insights insights) {
        int randomPairs =
                insights.byOrigin().stream()
                        .filter(row -> row.origin() == Origin.RANDOM)
                        .mapToInt(Insights.ByOrigin::pairs)
                        .sum();

        if (randomPairs < 30) {
            return String.format(
                    "Todavía no se puede decir nada: solo %d parejas al azar con las que comparar."
                            + " Hacen falta unas 30.",
                    randomPairs);
        }

        double lift = insights.liftOverRandom();
        if (lift == 0) {
            return "Ninguna pareja al azar ha conectado todavía.";
        }
        if (lift > 1.1) {
            return String.format(
                    Locale.ROOT,
                    "Las parejas elegidas conectan un %.0f%% más que las del azar.",
                    (lift - 1) * 100);
        }
        if (lift < 0.9) {
            return String.format(
                    Locale.ROOT,
                    "Cuidado: las elegidas conectan un %.0f%% MENOS que las del azar.",
                    (1 - lift) * 100);
        }
        return "Las elegidas y las del azar van igual: los pesos no están aportando nada.";
    }

    private static String spanish(Origin origin) {
        return switch (origin) {
            case BEST_MATCH -> "MEJOR PAREJA";
            case DISCOVERY -> "DESCUBRIMIENTO";
            case RANDOM -> "AZAR";
        };
    }
}
