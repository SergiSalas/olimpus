package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Each interest weighs according to how rare it is among the people actually
 * there.
 *
 * <p>Two people sharing "travel" says nothing: most people pick it. Sharing
 * "ice-climbing" is a huge signal. Counting shared interests without weighting
 * them throws away almost all of that information.
 */
public final class InterestWeights {

    private final Map<String, Double> weights;

    private InterestWeights(Map<String, Double> weights) {
        this.weights = weights;
    }

    /**
     * Computed over the real population, not a fixed table: on a campus
     * "climbing" may be common and in another city extremely rare.
     */
    public static InterestWeights fromPopulation(Collection<Profile> population) {
        Map<String, Integer> counts = new HashMap<>();
        for (Profile p : population) {
            for (String interest : p.interests()) {
                counts.merge(interest, 1, Integer::sum);
            }
        }
        int n = population.size();
        Map<String, Double> weights = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            // +1 on top and bottom so an interest nobody picked does not blow up
            // the logarithm, and so the most common one still weighs something.
            weights.put(entry.getKey(), Math.log((n + 1.0) / (entry.getValue() + 1.0)));
        }
        return new InterestWeights(weights);
    }

    /** Every interest weighs the same. Tests only. */
    public static InterestWeights uniform() {
        return new InterestWeights(Map.of());
    }

    public double weight(String interest) {
        return weights.getOrDefault(interest, 1.0);
    }

    /**
     * Similarity between two sets of interests, from 0 to 1.
     *
     * <p>It divides by the square root of the product of both totals (cosine-like
     * similarity) so someone with eight interests does not win by volume over
     * someone with five.
     */
    public double similarity(Set<String> mine, Set<String> theirs) {
        double shared = 0;
        for (String interest : mine) {
            if (theirs.contains(interest)) {
                shared += weight(interest);
            }
        }
        if (shared == 0) return 0;

        double normMine = mine.stream().mapToDouble(this::weight).sum();
        double normTheirs = theirs.stream().mapToDouble(this::weight).sum();
        if (normMine == 0 || normTheirs == 0) return 0;

        return Math.min(1, shared / Math.sqrt(normMine * normTheirs));
    }
}
