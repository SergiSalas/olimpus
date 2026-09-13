package com.sergisalas.olimpus.matching.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything matching needs to know that is not in the profile: who blocked
 * whom, who talked to whom and when, and how long each person has been waiting.
 *
 * <p>It is data, not a service: an adapter fills it before the round and the
 * domain only reads it. That way matching can be tested without a database.
 */
public final class MatchContext {

    private final LocalDate today;
    private final InterestWeights interestWeights;
    private final Set<PairKey> blocked;
    private final Map<PairKey, LocalDate> lastTalked;
    private final Map<UUID, Integer> conversations;
    private final Map<UUID, Integer> daysWaiting;

    private MatchContext(Builder builder) {
        this.today = builder.today;
        this.interestWeights = builder.interestWeights;
        this.blocked = Set.copyOf(builder.blocked);
        this.lastTalked = Map.copyOf(builder.lastTalked);
        this.conversations = Map.copyOf(builder.conversations);
        this.daysWaiting = Map.copyOf(builder.daysWaiting);
    }

    public static Builder on(LocalDate today) {
        return new Builder(today);
    }

    public LocalDate today() {
        return today;
    }

    public InterestWeights interestWeights() {
        return interestWeights;
    }

    /** Block or report in either direction. */
    public boolean isBlockedEitherWay(UUID a, UUID b) {
        return blocked.contains(PairKey.of(a, b));
    }

    /** Days since those two talked, or null if they never have. */
    public Integer daysSinceTalked(UUID a, UUID b) {
        LocalDate last = lastTalked.get(PairKey.of(a, b));
        if (last == null) return null;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(last, today);
    }

    public int conversationsOf(UUID id) {
        return conversations.getOrDefault(id, 0);
    }

    /** Days someone has gone without getting a conversation. */
    public int daysWaiting(UUID id) {
        return daysWaiting.getOrDefault(id, 0);
    }

    public static final class Builder {
        private final LocalDate today;
        private InterestWeights interestWeights = InterestWeights.uniform();
        private final Set<PairKey> blocked = new HashSet<>();
        private final Map<PairKey, LocalDate> lastTalked = new HashMap<>();
        private final Map<UUID, Integer> conversations = new HashMap<>();
        private final Map<UUID, Integer> daysWaiting = new HashMap<>();

        private Builder(LocalDate today) {
            this.today = today;
        }

        public Builder interestWeights(InterestWeights weights) {
            this.interestWeights = weights;
            return this;
        }

        public Builder blocked(UUID a, UUID b) {
            blocked.add(PairKey.of(a, b));
            return this;
        }

        public Builder talked(UUID a, UUID b, LocalDate day) {
            lastTalked.put(PairKey.of(a, b), day);
            return this;
        }

        public Builder conversations(UUID id, int count) {
            conversations.put(id, count);
            return this;
        }

        public Builder waiting(UUID id, int days) {
            daysWaiting.put(id, days);
            return this;
        }

        public MatchContext build() {
            return new MatchContext(this);
        }
    }
}
