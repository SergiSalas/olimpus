package com.sergisalas.olimpus.profile.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de intereses con su popularidad, copiado del laboratorio.
 *
 * <p>La cola larga es deliberada: que dos personas compartan "viajar" no dice
 * nada, porque lo marca la mayoria. Que compartan "escalada-en-hielo" es una
 * senal enorme, y el algoritmo pesa cada interes por lo raro que es.
 */
public final class InterestCatalog {

    public record Entry(String name, double popularity) {}

    public static final List<Entry> ENTRIES =
            List.of(
                    new Entry("viajar", 0.62),
                    new Entry("musica", 0.58),
                    new Entry("cine", 0.50),
                    new Entry("cocinar", 0.42),
                    new Entry("series", 0.40),
                    new Entry("leer", 0.36),
                    new Entry("deporte", 0.34),
                    new Entry("salir-de-tapas", 0.32),
                    new Entry("fotografia", 0.26),
                    new Entry("senderismo", 0.24),
                    new Entry("conciertos", 0.22),
                    new Entry("videojuegos", 0.22),
                    new Entry("arte", 0.19),
                    new Entry("correr", 0.18),
                    new Entry("gimnasio", 0.18),
                    new Entry("animales", 0.17),
                    new Entry("teatro", 0.14),
                    new Entry("jardineria", 0.12),
                    new Entry("podcasts", 0.12),
                    new Entry("bailar", 0.12),
                    new Entry("tocar-guitarra", 0.10),
                    new Entry("surf", 0.09),
                    new Entry("ciclismo", 0.09),
                    new Entry("historia", 0.08),
                    new Entry("astronomia", 0.07),
                    new Entry("escalada", 0.07),
                    new Entry("ajedrez", 0.06),
                    new Entry("ceramica", 0.05),
                    new Entry("buceo", 0.045),
                    new Entry("juegos-de-mesa", 0.045),
                    new Entry("filosofia", 0.04),
                    new Entry("vinos", 0.04),
                    new Entry("improvisacion", 0.03),
                    new Entry("apicultura", 0.02),
                    new Entry("escalada-en-hielo", 0.015),
                    new Entry("luthier", 0.012),
                    new Entry("birdwatching", 0.012),
                    new Entry("kendo", 0.010));

    private static final Map<String, Double> POR_NOMBRE = new LinkedHashMap<>();

    static {
        for (Entry entry : ENTRIES) {
            POR_NOMBRE.put(entry.name(), entry.popularity());
        }
    }

    private InterestCatalog() {}

    public static boolean contains(String name) {
        return POR_NOMBRE.containsKey(name);
    }

    /**
     * Que parte de la gente se espera que marque ese interes. Un interes que no
     * este en el catalogo se trata como comun, para no premiar por error algo
     * desconocido.
     */
    public static double popularityOf(String name) {
        return POR_NOMBRE.getOrDefault(name, 1.0);
    }

    public static int size() {
        return ENTRIES.size();
    }
}
