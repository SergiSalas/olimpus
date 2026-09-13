package com.sergisalas.olimpus.matching.domain;

/**
 * Where a pair comes from. It is stored with every conversation, and it is what
 * will answer the important question: do the pairs the algorithm picks work
 * better than random ones?
 */
public enum Origin {
    /** The best available pair according to the score. */
    BEST_MATCH,
    /** Good, but not the first: keeps anyone from being locked into the usual. */
    DISCOVERY,
    /** Random among those who pass the filters. It is the baseline to beat. */
    RANDOM
}
