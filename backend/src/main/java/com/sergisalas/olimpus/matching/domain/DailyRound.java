package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The matching of one round: who talks to whom today.
 *
 * <p>The score says <i>which</i> pairs are good; this decides <i>who gets
 * whom</i>, which is a different question. Each person appears in a single pair:
 * in Olimpus there is one new conversation a day.
 *
 * <p>The round is split in three parts, and each pair remembers which one it
 * came from:
 *
 * <ul>
 *   <li><b>80% best match</b>: every valid pair is scored and they are taken
 *       from highest to lowest.
 *   <li><b>10% discovery</b>: good, but not the first, so nobody gets locked
 *       into the usual.
 *   <li><b>10% pure random</b>: random among those who pass the filters. It is
 *       the baseline to beat.
 * </ul>
 *
 * <p>Hard filters apply to the random part <b>too</b>: randomness is about who
 * you talk to, never about safety.
 */
public final class DailyRound {

    private DailyRound() {}

    public static final double RANDOM_SHARE = 0.10;
    public static final double DISCOVERY_SHARE = 0.10;

    /**
     * Discovery picks from the top of the list, but never the first: from
     * position 1 up to the best 15%.
     *
     * <p>If it picked from the middle, discovery would be almost the same as
     * random, and two parts of the round would be measuring the same thing.
     */
    private static final double DISCOVERY_ZONE_UP_TO = 0.15;

    /**
     * @param rng same seed, same matching: a round can be replayed and reviewed
     */
    public static List<Match> plan(List<Profile> pool, MatchContext ctx, Random rng) {
        if (pool.size() < 2) return List.of();

        List<Profile> order = new ArrayList<>(pool);
        Collections.shuffle(order, rng);

        // "Seeds" are chosen, and each one takes a partner. That is why it is
        // divided by two: 200 people make about 100 pairs, and 10 random seeds
        // produce the 10 random pairs we are after.
        int randomCount = (int) Math.round(pool.size() * RANDOM_SHARE / 2);
        int discoveryCount = (int) Math.round(pool.size() * DISCOVERY_SHARE / 2);

        Set<UUID> forRandom = idsOf(order.subList(0, Math.min(randomCount, order.size())));
        Set<UUID> forDiscovery =
                idsOf(
                        order.subList(
                                Math.min(randomCount, order.size()),
                                Math.min(randomCount + discoveryCount, order.size())));

        Set<UUID> matched = new HashSet<>();
        List<Match> result = new ArrayList<>();

        // 1. Random first, so it really gets its share and not the leftovers
        //    of everything else.
        for (Profile person : order) {
            if (!forRandom.contains(person.accountId()) || matched.contains(person.accountId())) {
                continue;
            }
            List<ScoredPair> candidates = candidatesOf(person, pool, ctx, matched);
            if (candidates.isEmpty()) continue;

            ScoredPair chosen = candidates.get(rng.nextInt(candidates.size()));
            addPair(result, matched, chosen, Origin.RANDOM);
        }

        // 2. Discovery: from near the top of their list, never the first.
        for (Profile person : order) {
            if (!forDiscovery.contains(person.accountId()) || matched.contains(person.accountId())) {
                continue;
            }
            List<ScoredPair> candidates = candidatesOf(person, pool, ctx, matched);
            if (candidates.isEmpty()) continue;

            candidates.sort((p, q) -> Double.compare(q.score(), p.score()));
            addPair(result, matched, almostTheBest(candidates, rng), Origin.DISCOVERY);
        }

        // 3. The rest: every valid pair, from highest to lowest.
        //
        //    It is a greedy pass over the weight, not Gale-Shapley: here anyone
        //    can be paired with anyone (there are no two sides), and that problem
        //    does not always have a stable solution. At city scale this is plenty.
        List<ScoredPair> all = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                Profile a = pool.get(i);
                Profile b = pool.get(j);
                if (matched.contains(a.accountId()) || matched.contains(b.accountId())) {
                    continue;
                }
                if (!isValid(a, b, ctx)) continue;
                all.add(Scorer.score(a, b, ctx));
            }
        }
        all.sort((p, q) -> Double.compare(q.score(), p.score()));

        for (ScoredPair pair : all) {
            if (matched.contains(pair.a().accountId()) || matched.contains(pair.b().accountId())) {
                continue;
            }
            addPair(result, matched, pair, Origin.BEST_MATCH);
        }

        return result;
    }

    /** Who was left without a pair. It is the list that feeds the second-chance round. */
    public static List<UUID> leftOut(List<Profile> pool, List<Match> matches) {
        return pool.stream()
                .map(Profile::accountId)
                .filter(id -> matches.stream().noneMatch(m -> m.involves(id)))
                .toList();
    }

    private static boolean isValid(Profile a, Profile b, MatchContext ctx) {
        return Filters.passesHard(a, b, ctx)
                && Filters.passesSoft(a, b, ctx, Filters.relaxationFor(a, b, ctx));
    }

    private static List<ScoredPair> candidatesOf(
            Profile person, List<Profile> pool, MatchContext ctx, Set<UUID> matched) {
        List<ScoredPair> candidates = new ArrayList<>();
        for (Profile other : pool) {
            if (matched.contains(other.accountId())) continue;
            if (!isValid(person, other, ctx)) continue;
            candidates.add(Scorer.score(person, other, ctx));
        }
        return candidates;
    }

    /**
     * Picks someone good from the already sorted list, but not the first. If
     * there is only one option it takes it: discovery must never leave anyone
     * without a conversation.
     */
    private static ScoredPair almostTheBest(List<ScoredPair> sorted, Random rng) {
        if (sorted.size() == 1) return sorted.get(0);
        int upTo =
                Math.min(sorted.size(), Math.max(2, (int) Math.ceil(sorted.size() * DISCOVERY_ZONE_UP_TO)));
        return sorted.get(1 + rng.nextInt(upTo - 1));
    }

    private static void addPair(
            List<Match> result, Set<UUID> matched, ScoredPair pair, Origin origin) {
        result.add(Match.from(pair, origin));
        matched.add(pair.a().accountId());
        matched.add(pair.b().accountId());
    }

    private static Set<UUID> idsOf(List<Profile> profiles) {
        return profiles.stream().map(Profile::accountId).collect(Collectors.toSet());
    }
}
