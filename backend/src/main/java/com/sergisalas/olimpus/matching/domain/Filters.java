package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;

/**
 * Los filtros son dos capas y no son intercambiables.
 *
 * <p>Los <b>duros</b> no ceden nunca, por mucho que alguien lleve esperando. Si
 * alguno de ellos se relajara alguna vez, seria un fallo de seguridad, no una
 * mejora del producto.
 *
 * <p>Los <b>blandos</b> son preferencias, y tienen que ceder cuando hay poca
 * gente: son filtros, no promesas. Ceden poco a poco con los dias de espera, y
 * a la persona se le avisa ("ampliando radio a 30 km").
 */
public final class Filters {

    private Filters() {}

    /** Nivel de idioma compartido que se exige sin espera, y el minimo absoluto. */
    private static final double NIVEL_IDIOMA_BASE = 0.70;

    private static final double NIVEL_IDIOMA_MINIMO = 0.35;

    /** A los tres dias esperando ya estamos en la maxima relajacion. */
    public static final int DIAS_HASTA_MAXIMA_RELAJACION = 3;

    public static boolean passesHard(Profile a, Profile b, MatchContext ctx) {
        if (a.accountId().equals(b.accountId())) return false;

        // La mayoria de edad ya la garantiza Profile al construirse, pero el
        // reparto no se fia de nadie: si un perfil antiguo colara, aqui se corta.
        if (a.isMinorOn(ctx.today()) || b.isMinorOn(ctx.today())) return false;

        // La preferencia de genero tiene que encajar por los DOS lados.
        if (!a.seeking().contains(b.gender())) return false;
        if (!b.seeking().contains(a.gender())) return false;

        // Bloqueos y reportes, en cualquier direccion.
        return !ctx.isBlockedEitherWay(a.accountId(), b.accountId());
    }

    /**
     * @param relax 0 = las preferencias tal cual se pidieron, 1 = lo maximo que
     *     se admite
     */
    public static boolean passesSoft(Profile a, Profile b, MatchContext ctx, double relax) {
        double r = clamp01(relax);

        // Distancia: hasta el triple del radio pedido, y mutua.
        double km = a.location().distanceKmTo(b.location());
        if (km > a.maxDistanceKm() * (1 + 2 * r)) return false;
        if (km > b.maxDistanceKm() * (1 + 2 * r)) return false;

        // Rango de edad: se ensancha hasta cinco anos por lado, y mutuo. Nunca
        // por debajo de 18, porque eso lo impide el filtro duro.
        int edadA = a.ageOn(ctx.today());
        int edadB = b.ageOn(ctx.today());
        double holgura = 5 * r;
        if (edadB < a.ageMin() - holgura || edadB > a.ageMax() + holgura) return false;
        if (edadA < b.ageMin() - holgura || edadA > b.ageMax() + holgura) return false;

        // Idioma: se baja el liston con la espera, pero nunca por debajo del
        // punto en el que ya no hay conversacion posible.
        if (sharedLanguageLevel(a, b) < requiredLanguageLevel(r)) return false;

        // Intencion: con la maxima relajacion pasa cualquier combinacion.
        return IntentFit.between(a.intent(), b.intent()) >= 0.3 * (1 - r);
    }

    public static double requiredLanguageLevel(double relax) {
        return Math.max(NIVEL_IDIOMA_MINIMO, NIVEL_IDIOMA_BASE - 0.35 * clamp01(relax));
    }

    /**
     * Cuanto se relajan las preferencias de alguien que lleva dias esperando.
     * Crece de golpe el primer dia y satura a los tres.
     */
    public static double relaxationForDaysWaiting(int days) {
        return clamp01(days / (double) DIAS_HASTA_MAXIMA_RELAJACION);
    }

    /**
     * La relajacion de una pareja es la del que mas ha esperado: si a uno le
     * urge, se le abre la puerta aunque el otro acabe de llegar.
     */
    public static double relaxationFor(Profile a, Profile b, MatchContext ctx) {
        return Math.max(
                relaxationForDaysWaiting(ctx.daysWaiting(a.accountId())),
                relaxationForDaysWaiting(ctx.daysWaiting(b.accountId())));
    }

    /**
     * Mejor idioma en comun: manda la fluidez del lado mas flojo, porque la
     * conversacion la limita quien peor lo habla.
     */
    public static double sharedLanguageLevel(Profile a, Profile b) {
        double best = 0;
        for (var mine : a.languages()) {
            for (var theirs : b.languages()) {
                if (mine.code().equals(theirs.code())) {
                    best = Math.max(best, Math.min(mine.fluency(), theirs.fluency()));
                }
            }
        }
        return best;
    }

    private static double clamp01(double v) {
        return Math.min(1, Math.max(0, v));
    }
}
