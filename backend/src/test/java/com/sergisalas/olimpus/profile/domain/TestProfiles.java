package com.sergisalas.olimpus.profile.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A valid profile that each test changes only in what it wants to check.
 * Without it, every test would have to repeat every sign-up answer.
 */
public final class TestProfiles {

    public static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final LocalDate TODAY = LocalDate.of(2026, 9, 12);

    private TestProfiles() {}

    public static Builder valid() {
        return new Builder();
    }

    public static final class Builder {
        private UUID accountId = ACCOUNT;
        private String nickname = "Sergi";
        private LocalDate birthDate = LocalDate.of(1995, 3, 20);
        private Gender gender = Gender.MAN;
        private Set<Gender> seeking = Set.of(Gender.WOMAN);
        private int ageMin = 25;
        private int ageMax = 40;
        private int maxDistanceKm = 15;
        private Location location = Location.rounded(41.3874, 2.1686);
        private List<LanguageSkill> languages =
                List.of(
                        new LanguageSkill("es", LanguageSkill.Level.NATIVE),
                        new LanguageSkill("en", LanguageSkill.Level.INTERMEDIATE));
        private int sociability = 4;
        private int conversationDepth = 4;
        private Intent intent = Intent.RELATIONSHIP;
        private Set<String> interests =
                Set.of("climbing", "movies", "cooking", "astronomy", "chess");
        private String genderLabel = "";
        private List<PromptAnswer> prompts =
                List.of(
                        new PromptAnswer("last-hooked", "A book about lighthouses."),
                        new PromptAnswer("always-ask", "What did you have for breakfast?"),
                        new PromptAnswer("perfect-tuesday", "The sea and nothing else."));
        private String occupation = "";
        private String fromPlace = "";

        public Builder nickname(String v) {
            nickname = v;
            return this;
        }

        public Builder genderLabel(String v) {
            genderLabel = v;
            return this;
        }

        public Builder prompts(List<PromptAnswer> v) {
            prompts = v;
            return this;
        }

        public Builder occupation(String v) {
            occupation = v;
            return this;
        }

        public Builder fromPlace(String v) {
            fromPlace = v;
            return this;
        }

        public Builder birthDate(LocalDate v) {
            birthDate = v;
            return this;
        }

        public Builder seeking(Set<Gender> v) {
            seeking = v;
            return this;
        }

        public Builder ages(int min, int max) {
            ageMin = min;
            ageMax = max;
            return this;
        }

        public Builder maxDistanceKm(int v) {
            maxDistanceKm = v;
            return this;
        }

        public Builder location(Location v) {
            location = v;
            return this;
        }

        public Builder languages(List<LanguageSkill> v) {
            languages = v;
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

        public Builder interests(Set<String> v) {
            interests = v;
            return this;
        }

        public Profile build() {
            return new Profile(
                    accountId,
                    nickname,
                    birthDate,
                    gender,
                    genderLabel,
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
                    prompts,
                    occupation,
                    fromPlace);
        }
    }
}
