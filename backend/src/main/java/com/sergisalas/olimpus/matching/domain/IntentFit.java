package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Intent;

/**
 * How well two intents fit.
 *
 * <p>The table is not a judgement about what anyone is looking for: two people
 * with very different intents abandon the conversation, and abandonment is
 * exactly what matching tries to avoid.
 */
public final class IntentFit {

    private IntentFit() {}

    // Indexed by enum order. Symmetric; a test checks it.
    private static final double[][] TABLE = {
        /*                friendship dating relationship casual */
        /* friendship   */ {1.00, 0.35, 0.15, 0.20},
        /* dating       */ {0.35, 1.00, 0.75, 0.50},
        /* relationship */ {0.15, 0.75, 1.00, 0.10},
        /* casual       */ {0.20, 0.50, 0.10, 1.00}
    };

    public static double between(Intent a, Intent b) {
        return TABLE[a.ordinal()][b.ordinal()];
    }
}
