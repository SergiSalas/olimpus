package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Gender;
import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.InterestCatalog;
import com.sergisalas.olimpus.profile.domain.LanguageSkill;
import com.sergisalas.olimpus.profile.domain.Location;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.PromptAnswer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** A factory of people for the matching tests. */
public final class TestPeople {

    /** Matching never reads the answers, only needs them to be there. */
    private static final List<PromptAnswer> PROMPTS =
            List.of(
                    new PromptAnswer("last-hooked", "A book about lighthouses."),
                    new PromptAnswer("always-ask", "What did you have for breakfast?"),
                    new PromptAnswer("perfect-tuesday", "The sea and nothing else."));

    public static final LocalDate TODAY = LocalDate.of(2026, 9, 12);

    /** Plaça de Catalunya, Barcelona. */
    public static final Location CENTER = Location.rounded(41.3874, 2.1686);

    private TestPeople() {}

    public static Builder person() {
        return new Builder();
    }

    /** Someone aged 30 who is open to anyone and whom everyone fits. */
    public static Profile anyone() {
        return person().build();
    }

    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private String nickname = "Someone";
        private int age = 30;
        private Gender gender = Gender.WOMAN;
        private Set<Gender> seeking = Set.of(Gender.WOMAN, Gender.MAN, Gender.NON_BINARY, Gender.OTHER);
        private int ageMin = 18;
        private int ageMax = 99;
        private int maxDistanceKm = 50;
        private Location location = CENTER;
        private List<LanguageSkill> languages =
                List.of(new LanguageSkill("es", LanguageSkill.Level.NATIVE));
        private int sociability = 3;
        private int conversationDepth = 3;
        private Intent intent = Intent.DATING;
        private Set<String> interests = Set.of("movies", "reading", "running", "surfing", "wine");

        public Builder id(UUID v) {
            id = v;
            return this;
        }

        public Builder nickname(String v) {
            nickname = v;
            return this;
        }

        public Builder age(int v) {
            age = v;
            return this;
        }

        public Builder gender(Gender v) {
            gender = v;
            return this;
        }

        public Builder seeking(Gender... v) {
            seeking = Set.of(v);
            return this;
        }

        public Builder ageRange(int min, int max) {
            ageMin = min;
            ageMax = max;
            return this;
        }

        public Builder maxDistanceKm(int km) {
            maxDistanceKm = km;
            return this;
        }

        public Builder at(double lat, double lon) {
            location = Location.rounded(lat, lon);
            return this;
        }

        public Builder languages(LanguageSkill... v) {
            languages = List.of(v);
            return this;
        }

        public Builder sociability(int v) {
            sociability = v;
            return this;
        }

        public Builder conversationDepth(int v) {
            conversationDepth = v;
            return this;
        }

        public Builder intent(Intent v) {
            intent = v;
            return this;
        }

        public Builder interests(String... v) {
            interests = Set.of(v);
            return this;
        }

        public Profile build() {
            return new Profile(
                    id,
                    nickname,
                    TODAY.minusYears(age).minusDays(1),
                    gender,
                    "",
                    seeking,
                    ageMin,
                    ageMax,
                    maxDistanceKm,
                    location,
                    languages,
                    sociability,
                    conversationDepth,
                    intent,
                    interests,
                    PROMPTS,
                    "",
                    "");
        }
    }

    /**
     * A varied and reproducible population: ages, genders, sociability and
     * interests spread out. Used to test matching at scale.
     */
    public static List<Profile> population(int count, long seed) {
        Random rng = new Random(seed);
        Gender[] genders = {Gender.WOMAN, Gender.MAN, Gender.NON_BINARY};
        Intent[] intents = {Intent.FRIENDSHIP, Intent.DATING, Intent.RELATIONSHIP, Intent.CASUAL};
        List<String> catalog =
                InterestCatalog.ENTRIES.stream().map(InterestCatalog.Entry::name).toList();

        List<Profile> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Set<String> interests = new LinkedHashSet<>();
            while (interests.size() < 5 + rng.nextInt(4)) {
                interests.add(catalog.get(rng.nextInt(catalog.size())));
            }
            people.add(
                    person()
                            .nickname("P" + i)
                            .age(20 + rng.nextInt(25))
                            .gender(genders[rng.nextInt(genders.length)])
                            .ageRange(18, 99)
                            .maxDistanceKm(50)
                            .at(41.3874 + (rng.nextDouble() - 0.5) * 0.1, 2.1686 + (rng.nextDouble() - 0.5) * 0.1)
                            .sociability(1 + rng.nextInt(5))
                            .conversationDepth(1 + rng.nextInt(5))
                            .intent(intents[rng.nextInt(intents.length)])
                            .interests(interests.toArray(String[]::new))
                            .build());
        }
        return people;
    }
}
