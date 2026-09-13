package com.sergisalas.olimpus.matching.domain;

/** The two rounds of the day. */
public enum RoundKind {
    /** The 4:00 one: matches everyone. */
    MAIN,
    /**
     * The 14:00 one: only for whoever was left without a pair or whose
     * conversation is still silent at midday. It keeps someone who tried the app
     * for a day from leaving with the feeling that nothing happens.
     */
    SECOND_CHANCE
}
