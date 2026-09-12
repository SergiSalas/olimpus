package com.sergisalas.olimpus.profile.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un perfil valido al que cada test le cambia solo lo que quiere probar. Sin
 * esto, cada test tendria que repetir las quince respuestas del registro.
 */
public final class TestProfiles {

    public static final UUID CUENTA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final LocalDate HOY = LocalDate.of(2026, 9, 12);

    private TestProfiles() {}

    public static Builder valido() {
        return new Builder();
    }

    public static final class Builder {
        private UUID accountId = CUENTA;
        private String nickname = "Sergi";
        private String bio = "Me gusta el mar.";
        private LocalDate birthDate = LocalDate.of(1995, 3, 20);
        private Gender gender = Gender.HOMBRE;
        private Set<Gender> seeking = Set.of(Gender.MUJER);
        private int ageMin = 25;
        private int ageMax = 40;
        private int maxDistanceKm = 15;
        private Location location = Location.rounded(41.3874, 2.1686);
        private List<LanguageSkill> languages =
                List.of(
                        new LanguageSkill("es", LanguageSkill.Level.NATIVO),
                        new LanguageSkill("en", LanguageSkill.Level.MEDIO));
        private int sociability = 4;
        private int conversationDepth = 4;
        private Intent intent = Intent.PAREJA;
        private Set<String> interests =
                Set.of("escalada", "cine", "cocinar", "astronomia", "ajedrez");

        public Builder nickname(String v) {
            nickname = v;
            return this;
        }

        public Builder bio(String v) {
            bio = v;
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
                    bio,
                    birthDate,
                    gender,
                    seeking,
                    ageMin,
                    ageMax,
                    maxDistanceKm,
                    location,
                    languages,
                    sociability,
                    conversationDepth,
                    intent,
                    interests);
        }
    }
}
