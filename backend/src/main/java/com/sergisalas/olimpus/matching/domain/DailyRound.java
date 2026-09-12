package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * El reparto de una ronda: quien habla con quien hoy.
 *
 * <p>La puntuacion dice <i>que</i> parejas son buenas; esto decide <i>quien se
 * lleva a quien</i>, que es otra pregunta. Cada persona sale en una sola pareja:
 * en Olimpus hay una conversacion nueva al dia.
 *
 * <p>El reparto se divide en tres partes, y cada pareja recuerda de cual salio:
 *
 * <ul>
 *   <li><b>80% mejor pareja</b>: se puntuan todas las parejas validas y se van
 *       cogiendo de mayor a menor.
 *   <li><b>10% descubrimiento</b>: buena, pero no la primera, para no encerrar a
 *       nadie en lo de siempre.
 *   <li><b>10% azar puro</b>: al azar entre quienes pasan los filtros. Es la
 *       referencia contra la que hay que batir.
 * </ul>
 *
 * <p>Los filtros duros se aplican <b>tambien</b> a la parte del azar: el azar es
 * de con quien hablas, nunca de la seguridad.
 */
public final class DailyRound {

    private DailyRound() {}

    public static final double FRACCION_AZAR = 0.10;
    public static final double FRACCION_DESCUBRIMIENTO = 0.10;

    /**
     * En descubrimiento se coge de la cabeza de la lista, pero nunca el primero:
     * de la posicion 1 hasta el 15% mejor.
     *
     * <p>Si se cogiera de la zona media, descubrir seria casi lo mismo que el
     * azar, y entonces habria dos partes del reparto midiendo lo mismo.
     */
    private static final double ZONA_DESCUBRIMIENTO_HASTA = 0.15;

    /**
     * @param rng con la misma semilla, el mismo reparto: una ronda se puede
     *     repetir y revisar
     */
    public static List<Match> plan(List<Profile> pool, MatchContext ctx, Random rng) {
        if (pool.size() < 2) return List.of();

        List<Profile> orden = new ArrayList<>(pool);
        Collections.shuffle(orden, rng);

        // Se eligen "cabezas de serie", y cada una se lleva a su pareja. Por eso
        // se divide entre dos: con 200 personas salen unas 100 parejas, y 10
        // cabezas de azar producen las 10 parejas de azar que buscamos.
        int cuantosAzar = (int) Math.round(pool.size() * FRACCION_AZAR / 2);
        int cuantosDescubrimiento = (int) Math.round(pool.size() * FRACCION_DESCUBRIMIENTO / 2);

        Set<UUID> paraAzar = idsDe(orden.subList(0, Math.min(cuantosAzar, orden.size())));
        Set<UUID> paraDescubrimiento =
                idsDe(
                        orden.subList(
                                Math.min(cuantosAzar, orden.size()),
                                Math.min(cuantosAzar + cuantosDescubrimiento, orden.size())));

        Set<UUID> emparejados = new HashSet<>();
        List<Match> resultado = new ArrayList<>();

        // 1. El azar primero, para que se lleve de verdad su parte y no las
        //    sobras de lo demas.
        for (Profile persona : orden) {
            if (!paraAzar.contains(persona.accountId()) || emparejados.contains(persona.accountId())) {
                continue;
            }
            List<ScoredPair> candidatos = candidatosDe(persona, pool, ctx, emparejados);
            if (candidatos.isEmpty()) continue;

            ScoredPair elegida = candidatos.get(rng.nextInt(candidatos.size()));
            apuntar(resultado, emparejados, elegida, Origin.AZAR);
        }

        // 2. Descubrimiento: de la zona media de su lista.
        for (Profile persona : orden) {
            if (!paraDescubrimiento.contains(persona.accountId())
                    || emparejados.contains(persona.accountId())) {
                continue;
            }
            List<ScoredPair> candidatos = candidatosDe(persona, pool, ctx, emparejados);
            if (candidatos.isEmpty()) continue;

            candidatos.sort((p, q) -> Double.compare(q.score(), p.score()));
            apuntar(resultado, emparejados, casiElMejor(candidatos, rng), Origin.DESCUBRIMIENTO);
        }

        // 3. El resto: todas las parejas validas, de mayor a menor.
        //
        //    Es un "greedy" sobre el peso, no Gale-Shapley: aqui cualquiera puede
        //    emparejarse con cualquiera (no hay dos lados), y ese problema no
        //    siempre tiene solucion estable. A la escala de una ciudad esto sobra.
        List<ScoredPair> todas = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                Profile a = pool.get(i);
                Profile b = pool.get(j);
                if (emparejados.contains(a.accountId()) || emparejados.contains(b.accountId())) {
                    continue;
                }
                if (!valida(a, b, ctx)) continue;
                todas.add(Scorer.score(a, b, ctx));
            }
        }
        todas.sort((p, q) -> Double.compare(q.score(), p.score()));

        for (ScoredPair pareja : todas) {
            if (emparejados.contains(pareja.a().accountId())
                    || emparejados.contains(pareja.b().accountId())) {
                continue;
            }
            apuntar(resultado, emparejados, pareja, Origin.MEJOR_PAREJA);
        }

        return resultado;
    }

    /** Quien se ha quedado sin pareja. Es la lista que alimenta la repesca. */
    public static List<UUID> leftOut(List<Profile> pool, List<Match> matches) {
        return pool.stream()
                .map(Profile::accountId)
                .filter(id -> matches.stream().noneMatch(m -> m.involves(id)))
                .toList();
    }

    private static boolean valida(Profile a, Profile b, MatchContext ctx) {
        return Filters.passesHard(a, b, ctx)
                && Filters.passesSoft(a, b, ctx, Filters.relaxationFor(a, b, ctx));
    }

    private static List<ScoredPair> candidatosDe(
            Profile persona, List<Profile> pool, MatchContext ctx, Set<UUID> emparejados) {
        List<ScoredPair> candidatos = new ArrayList<>();
        for (Profile otro : pool) {
            if (emparejados.contains(otro.accountId())) continue;
            if (!valida(persona, otro, ctx)) continue;
            candidatos.add(Scorer.score(persona, otro, ctx));
        }
        return candidatos;
    }

    /**
     * Coge a alguien bueno de la lista ya ordenada, pero no al primero. Si solo
     * hay una opcion, se queda con ella: descubrir no puede costarle a nadie
     * quedarse sin conversacion.
     */
    private static ScoredPair casiElMejor(List<ScoredPair> ordenados, Random rng) {
        if (ordenados.size() == 1) return ordenados.get(0);
        int hasta =
                Math.min(ordenados.size(), Math.max(2, (int) Math.ceil(ordenados.size() * ZONA_DESCUBRIMIENTO_HASTA)));
        return ordenados.get(1 + rng.nextInt(hasta - 1));
    }

    private static void apuntar(
            List<Match> resultado, Set<UUID> emparejados, ScoredPair pareja, Origin origin) {
        resultado.add(Match.from(pareja, origin));
        emparejados.add(pareja.a().accountId());
        emparejados.add(pareja.b().accountId());
    }

    private static Set<UUID> idsDe(List<Profile> perfiles) {
        return perfiles.stream().map(Profile::accountId).collect(java.util.stream.Collectors.toSet());
    }
}
