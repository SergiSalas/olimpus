package com.sergisalas.olimpus.profile.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue of interests with their popularity, copied from the lab.
 *
 * <p>The long tail is deliberate: two people sharing "travel" says nothing,
 * because most people pick it. Sharing "ice-climbing" is a huge signal, and the
 * algorithm weighs each interest by how rare it is.
 *
 * <p>Ids are language-neutral; the label each person reads lives in
 * {@code messages.properties} under {@code interest.<id>}.
 */
public final class InterestCatalog {

    public record Entry(String name, double popularity) {}

    public static final List<Entry> ENTRIES =
            List.of(
                    new Entry("travel", 0.62),
                    new Entry("music", 0.58),
                    new Entry("movies", 0.50),
                    new Entry("cooking", 0.42),
                    new Entry("tv-series", 0.40),
                    new Entry("reading", 0.36),
                    new Entry("sports", 0.34),
                    new Entry("tapas", 0.32),
                    new Entry("photography", 0.26),
                    new Entry("hiking", 0.24),
                    new Entry("concerts", 0.22),
                    new Entry("video-games", 0.22),
                    new Entry("art", 0.19),
                    new Entry("running", 0.18),
                    new Entry("gym", 0.18),
                    new Entry("animals", 0.17),
                    new Entry("theatre", 0.14),
                    new Entry("gardening", 0.12),
                    new Entry("podcasts", 0.12),
                    new Entry("dancing", 0.12),
                    new Entry("guitar", 0.10),
                    new Entry("surfing", 0.09),
                    new Entry("cycling", 0.09),
                    new Entry("history", 0.08),
                    new Entry("astronomy", 0.07),
                    new Entry("climbing", 0.07),
                    new Entry("chess", 0.06),
                    new Entry("ceramics", 0.05),
                    new Entry("diving", 0.045),
                    new Entry("board-games", 0.045),
                    new Entry("philosophy", 0.04),
                    new Entry("wine", 0.04),
                    new Entry("improv", 0.03),
                    new Entry("beekeeping", 0.02),
                    new Entry("ice-climbing", 0.015),
                    new Entry("instrument-making", 0.012),
                    new Entry("birdwatching", 0.012),
                    new Entry("kendo", 0.010));

    private static final Map<String, Double> BY_NAME = new LinkedHashMap<>();

    static {
        for (Entry entry : ENTRIES) {
            BY_NAME.put(entry.name(), entry.popularity());
        }
    }

    private InterestCatalog() {}

    public static boolean contains(String name) {
        return BY_NAME.containsKey(name);
    }

    /**
     * Share of people expected to pick that interest. An interest missing from
     * the catalogue is treated as common, so nothing unknown gets rewarded by
     * mistake.
     */
    public static double popularityOf(String name) {
        return BY_NAME.getOrDefault(name, 1.0);
    }

    public static int size() {
        return ENTRIES.size();
    }
}
