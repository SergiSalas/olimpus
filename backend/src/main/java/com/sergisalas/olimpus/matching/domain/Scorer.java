package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;

/**
 * Cuanto promete una pareja, de 0 a 1.
 *
 * <p>Los pesos son <b>hipotesis, no configuracion</b>. Estan puestos a mano con
 * lo que salio de los datos reales de citas rapidas de Columbia: lo que mas
 * pesaba era la edad cercana, despues los intereses raros compartidos, y
 * despues que los dos fueran sociables. Cuando la app tenga sus propios datos,
 * seran ellos los que digan cuales eran los pesos buenos.
 *
 * <p>Por eso el reparto guarda de donde sale cada pareja (ver {@link Origin}) y
 * una parte se hace al azar: si las parejas elegidas no funcionan mejor que las
 * del azar, estos numeros no estan aportando nada.
 */
public final class Scorer {

    private Scorer() {}

    public static final double W_EDAD_CERCANA = 0.28;
    public static final double W_INTERESES = 0.22;
    public static final double W_SOCIABILIDAD = 0.18;
    public static final double W_INTENCION = 0.12;
    public static final double W_IDIOMA = 0.08;
    public static final double W_EDAD_PEDIDA = 0.06;
    public static final double W_DISTANCIA = 0.04;
    public static final double W_NOVEDAD = 0.02;

    public static final double SUMA_PESOS =
            W_EDAD_CERCANA
                    + W_INTERESES
                    + W_SOCIABILIDAD
                    + W_INTENCION
                    + W_IDIOMA
                    + W_EDAD_PEDIDA
                    + W_DISTANCIA
                    + W_NOVEDAD;

    /**
     * Puntua la pareja mirando los dos lados y quedandose con el peor.
     *
     * <p>Es el criterio del documento: una pareja solo es buena si le conviene a
     * los dos. Con la media pasaria el caso de "a uno le encanta y al otro le da
     * igual", que acaba en conversacion muerta.
     */
    public static ScoredPair score(Profile a, Profile b, MatchContext ctx) {
        double ladoA = directional(a, b, ctx);
        double ladoB = directional(b, a, ctx);
        return new ScoredPair(a, b, Math.min(ladoA, ladoB), ladoA, ladoB);
    }

    /**
     * Cuanto le conviene b a la persona a. Es asimetrico a proposito: el rango
     * de edad y la distancia maxima se miden desde a.
     */
    public static double directional(Profile a, Profile b, MatchContext ctx) {
        double suma =
                W_EDAD_CERCANA * ageClosenessFit(a, b, ctx)
                        + W_INTERESES
                                * ctx.interestWeights().similarity(a.interests(), b.interests())
                        + W_SOCIABILIDAD * sociabilityFit(a, b)
                        + W_INTENCION * IntentFit.between(a.intent(), b.intent())
                        + W_IDIOMA * Filters.sharedLanguageLevel(a, b)
                        + W_EDAD_PEDIDA * requestedAgeFit(a, b, ctx)
                        + W_DISTANCIA * distanceFit(a, b)
                        + W_NOVEDAD * noveltyFit(a, b, ctx);

        return clamp01(suma / SUMA_PESOS);
    }

    /**
     * Edad parecida. Es el factor que mas peso tiene porque es el que mas
     * aparecia en los datos reales: cuatro anos de diferencia ya restan bastante,
     * doce dejan el factor casi a cero.
     */
    public static double ageClosenessFit(Profile a, Profile b, MatchContext ctx) {
        int diferencia = Math.abs(a.ageOn(ctx.today()) - b.ageOn(ctx.today()));
        return Math.exp(-diferencia / 6.0);
    }

    /**
     * Que los dos sean sociables, mas que se parezcan.
     *
     * <p>Dos personas muy calladas son "compatibles" en cualquier tabla de
     * parecido, y sin embargo producen silencio. Lo que si premia el parecido es
     * el tipo de conversacion: quien quiere charla ligera y quien quiere ir hondo
     * se desencuentran.
     */
    public static double sociabilityFit(Profile a, Profile b) {
        double nivelConjunto = (a.sociabilityScore() + b.sociabilityScore()) / 2;
        double mismoEstilo = 1 - Math.abs(a.conversationDepthScore() - b.conversationDepthScore());
        return 0.6 * nivelConjunto + 0.4 * mismoEstilo;
    }

    /** Cuanto encaja la edad de b en lo que a pidio. Vale 1 en el centro del rango. */
    public static double requestedAgeFit(Profile a, Profile b, MatchContext ctx) {
        int edadB = b.ageOn(ctx.today());
        if (edadB >= a.ageMin() && edadB <= a.ageMax()) {
            double centro = (a.ageMin() + a.ageMax()) / 2.0;
            double mitad = Math.max(1, (a.ageMax() - a.ageMin()) / 2.0);
            return 1 - 0.25 * (Math.abs(edadB - centro) / mitad);
        }
        int exceso = edadB < a.ageMin() ? a.ageMin() - edadB : edadB - a.ageMax();
        return Math.max(0, 0.75 - 0.15 * exceso);
    }

    public static double distanceFit(Profile a, Profile b) {
        double km = a.location().distanceKmTo(b.location());
        return Math.exp(-km / Math.max(1, a.maxDistanceKm()));
    }

    /**
     * Novedad: en vez de bloquear para siempre a quien ya hablo contigo, el peso
     * de volver a coincidir se recupera con los dias. Nunca hablado vale 1;
     * hablado hoy, casi 0; en un par de semanas ya vuelve a ser posible.
     */
    public static double noveltyFit(Profile a, Profile b, MatchContext ctx) {
        Integer dias = ctx.daysSinceTalked(a.accountId(), b.accountId());
        if (dias == null) return 1;
        return 1 - Math.exp(-Math.max(0, dias) / 10.0);
    }

    private static double clamp01(double v) {
        return Math.min(1, Math.max(0, v));
    }
}
