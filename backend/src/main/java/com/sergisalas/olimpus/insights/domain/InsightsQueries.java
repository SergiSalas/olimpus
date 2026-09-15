package com.sergisalas.olimpus.insights.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Port: the counting the database does better than any Java loop would.
 *
 * <p>The numbers come back raw. Turning them into rates and verdicts happens
 * above, where it can be read and tested without a database.
 */
public interface InsightsQueries {

    /** Pairs, conversations that took off, connections and survivors, per origin. */
    List<Insights.ByOrigin> outcomesBetween(LocalDate from, LocalDate to);

    int reportsBetween(LocalDate from, LocalDate to);

    int blocksBetween(LocalDate from, LocalDate to);

    /** Registered people who have not been matched with anyone in the last week. */
    int peopleWithNoMatchInAWeek(LocalDate today);

    /** Average days each registered person has been waiting for a conversation. */
    double averageDaysWaiting(LocalDate today);

    /** Pairs handed out that nobody ever wrote in. */
    int diedInSilenceBetween(LocalDate from, LocalDate to);

    /** How many conversations each person has had, to see how evenly they spread. */
    List<Integer> conversationsPerPerson(LocalDate from, LocalDate to);
}
