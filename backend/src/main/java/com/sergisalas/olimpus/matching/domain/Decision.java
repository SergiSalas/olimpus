package com.sergisalas.olimpus.matching.domain;

/**
 * What each person answers, alone, in the last minutes of the conversation:
 * "do you want to keep getting to know this person?".
 *
 * <p>It is asked before the conversation is cut, not after, because the feeling
 * is still alive. Once it is over, people answer coldly and out of politeness.
 */
public enum Decision {
    YES,
    NO
}
