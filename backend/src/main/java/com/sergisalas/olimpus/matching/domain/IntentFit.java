package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Intent;

/**
 * Cuanto encajan dos intenciones.
 *
 * <p>La tabla no es un juicio sobre lo que busca cada uno: es que dos personas
 * con intenciones muy distintas abandonan la conversacion, y el abandono es
 * justo lo que el reparto intenta evitar.
 */
public final class IntentFit {

    private IntentFit() {}

    // Indexada por el orden del enum. Simetrica; lo comprueba un test.
    private static final double[][] TABLA = {
        /*            amistad citas pareja casual */
        /* amistad */ {1.00, 0.35, 0.15, 0.20},
        /* citas   */ {0.35, 1.00, 0.75, 0.50},
        /* pareja  */ {0.15, 0.75, 1.00, 0.10},
        /* casual  */ {0.20, 0.50, 0.10, 1.00}
    };

    public static double between(Intent a, Intent b) {
        return TABLA[a.ordinal()][b.ordinal()];
    }
}
