package com.sergisalas.olimpus.insights.application;

import com.sergisalas.olimpus.insights.domain.Concentration;
import com.sergisalas.olimpus.insights.domain.Insights;
import com.sergisalas.olimpus.insights.domain.InsightsQueries;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Use case: how is Olimpus doing.
 *
 * <p>It answers over a window of days rather than "ever", because the numbers
 * that matter are the ones moving: a week of the beta says more than the
 * average since the beginning.
 */
public class MeasureOutcomes {

    private final InsightsQueries queries;
    private final RoundSchedule schedule;
    private final Clock clock;

    public MeasureOutcomes(InsightsQueries queries, RoundSchedule schedule, Clock clock) {
        this.queries = queries;
        this.schedule = schedule;
        this.clock = clock;
    }

    public Insights lastDays(int days) {
        LocalDate today = schedule.dateOf(clock.instant());
        LocalDate from = today.minusDays(Math.max(1, days) - 1L);

        List<Integer> perPerson = queries.conversationsPerPerson(from, today);

        return new Insights(
                from,
                today,
                queries.outcomesBetween(from, today),
                new Insights.Alarms(
                        queries.reportsBetween(from, today),
                        queries.blocksBetween(from, today),
                        queries.peopleWithNoMatchInAWeek(today),
                        queries.averageDaysWaiting(today),
                        queries.diedInSilenceBetween(from, today),
                        Concentration.shareTakenByTopTenth(perPerson),
                        Concentration.evenShare(perPerson.size())));
    }
}
