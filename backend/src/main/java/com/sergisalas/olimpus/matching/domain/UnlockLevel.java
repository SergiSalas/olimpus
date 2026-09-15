package com.sergisalas.olimpus.matching.domain;

/**
 * How much of the other person is visible. This ladder is what the whole app is
 * about: the profile is not shown, it is earned.
 */
public enum UnlockLevel {
    /** Age, two interests, roughly how far away. Given by the match itself. */
    MATCH(0),
    /** Nickname and every interest. Both have written at least once. */
    FIRST_MESSAGE(1),
    /** Short bio. Real back-and-forth, and time on top of it. */
    CONVERSATION(2),
    /** Photo and wider profile. Both said they want to see each other. */
    GOOD_CONNECTION(3),
    /** Whatever each one chooses to share. After the mutual yes at 22:00. */
    TRUST(4);

    private final int number;

    UnlockLevel(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public boolean atLeast(UnlockLevel other) {
        return number >= other.number;
    }
}
