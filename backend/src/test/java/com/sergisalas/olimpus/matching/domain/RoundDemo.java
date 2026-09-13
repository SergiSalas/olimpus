package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.TestPeople.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * It checks no new rules: it prints what a round looks like so a human can look
 * at it. The numbers printed here are the ones to compare with the real app once
 * there is real data.
 */
class RoundDemo {

    @Test
    void summary_of_a_round_of_two_hundred_people() {
        List<Profile> people = TestPeople.population(200, 2026);
        MatchContext ctx =
                MatchContext.on(TODAY).interestWeights(InterestWeights.fromPopulation(people)).build();

        List<Match> round = DailyRound.plan(people, ctx, new Random(2026));
        List<java.util.UUID> leftOut = DailyRound.leftOut(people, round);

        System.out.printf("%n=== ROUND OF %s ===%n", TODAY);
        System.out.printf("People in the round............ %d%n", people.size());
        System.out.printf("Pairs formed................... %d%n", round.size());
        System.out.printf(
                "Left without a conversation.... %d (%.0f%%)%n",
                leftOut.size(), 100.0 * leftOut.size() / people.size());

        System.out.printf("%n%-16s %8s %10s%n", "ORIGIN", "PAIRS", "PROMISE");
        for (Origin origin : Origin.values()) {
            List<Match> ofThatKind = round.stream().filter(m -> m.origin() == origin).toList();
            double average =
                    ofThatKind.stream().mapToDouble(Match::score).average().orElse(0);
            System.out.printf("%-16s %8d %9.2f%n", origin, ofThatKind.size(), average);
        }

        double chosen =
                round.stream()
                        .filter(m -> m.origin() == Origin.BEST_MATCH)
                        .mapToDouble(Match::score)
                        .average()
                        .orElse(0);
        double random =
                round.stream()
                        .filter(m -> m.origin() == Origin.RANDOM)
                        .mapToDouble(Match::score)
                        .average()
                        .orElse(0);

        System.out.printf(
                "%nChosen pairs promise %.0f%% more than random ones.%n",
                100 * (chosen / random - 1));
        System.out.println(
                "Careful: that is what the algorithm promises, not what actually happens.");
        System.out.println(
                "Who was right will be known with beta data, by comparing how many of");
        System.out.println("each kind are still talking 48 hours later.");
        System.out.println();

        assertThat(round).isNotEmpty();
    }
}
