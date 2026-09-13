package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What someone declares at sign-up: the ten questions.
 *
 * <p>It is only the starting point. Over time, how they actually behave (who
 * they keep talking to, who they connect with) should weigh more than what they
 * said on day one.
 *
 * <p>The rules live here, in the constructor: a profile that exists is a valid
 * profile. Nobody can store a half-filled one through another door.
 */
public record Profile(
        UUID accountId,
        String nickname,
        String bio,
        LocalDate birthDate,
        Gender gender,
        Set<Gender> seeking,
        int ageMin,
        int ageMax,
        int maxDistanceKm,
        Location location,
        List<LanguageSkill> languages,
        /** 1 = slow to warm up, 5 = talks to anyone. */
        int sociability,
        /** 1 = light and fun, 5 = the kind that goes deep. */
        int conversationDepth,
        Intent intent,
        Set<String> interests) {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 99;
    public static final int MIN_INTERESTS = 5;
    public static final int MAX_INTERESTS = 8;
    public static final int MAX_LANGUAGES = 5;
    public static final int MIN_NICKNAME = 2;
    public static final int MAX_NICKNAME = 20;
    public static final int MAX_BIO = 200;

    /** Distance is picked on a slider, in 5 km steps, so no odd numbers get invented. */
    public static final int MIN_DISTANCE_KM = 5;
    public static final int MAX_DISTANCE_KM = 120;
    public static final int DISTANCE_STEP_KM = 5;

    public Profile {
        if (accountId == null) throw new IllegalArgumentException("account is missing");

        nickname = nickname == null ? "" : nickname.trim();
        if (nickname.length() < MIN_NICKNAME || nickname.length() > MAX_NICKNAME) {
            throw new RuleViolationException("profile.nickname.length", MIN_NICKNAME, MAX_NICKNAME);
        }

        bio = bio == null ? "" : bio.trim();
        if (bio.length() > MAX_BIO) {
            throw new RuleViolationException("profile.bio.too-long", MAX_BIO);
        }

        if (birthDate == null) throw new RuleViolationException("profile.birth-date.missing");
        if (gender == null) throw new RuleViolationException("profile.gender.missing");

        if (seeking == null || seeking.isEmpty()) {
            throw new RuleViolationException("profile.seeking.empty");
        }
        seeking = Set.copyOf(seeking);

        if (ageMin < MIN_AGE || ageMax > MAX_AGE) {
            throw new RuleViolationException("profile.age-range.out-of-bounds", MIN_AGE, MAX_AGE);
        }
        if (ageMin > ageMax) {
            throw new RuleViolationException("profile.age-range.reversed");
        }

        if (maxDistanceKm < MIN_DISTANCE_KM
                || maxDistanceKm > MAX_DISTANCE_KM
                || maxDistanceKm % DISTANCE_STEP_KM != 0) {
            throw new RuleViolationException(
                    "profile.distance.invalid", MIN_DISTANCE_KM, MAX_DISTANCE_KM, DISTANCE_STEP_KM);
        }

        if (location == null) throw new RuleViolationException("profile.location.missing");

        if (languages == null || languages.isEmpty()) {
            throw new RuleViolationException("profile.languages.empty");
        }
        if (languages.size() > MAX_LANGUAGES) {
            throw new RuleViolationException("profile.languages.too-many", MAX_LANGUAGES);
        }
        languages = List.copyOf(languages);
        if (languages.stream().map(LanguageSkill::code).distinct().count() != languages.size()) {
            throw new RuleViolationException("profile.languages.duplicate");
        }

        if (sociability < 1 || sociability > 5) {
            throw new RuleViolationException("profile.scale.out-of-range");
        }
        if (conversationDepth < 1 || conversationDepth > 5) {
            throw new RuleViolationException("profile.scale.out-of-range");
        }

        if (intent == null) throw new RuleViolationException("profile.intent.missing");

        if (interests == null
                || interests.size() < MIN_INTERESTS
                || interests.size() > MAX_INTERESTS) {
            throw new RuleViolationException("profile.interests.count", MIN_INTERESTS, MAX_INTERESTS);
        }
        interests = Set.copyOf(interests);
        for (String interest : interests) {
            if (!InterestCatalog.contains(interest)) {
                throw new RuleViolationException("profile.interests.unknown", interest);
            }
        }
    }

    public int ageOn(LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    /**
     * A filter that never gives way. It is checked when saving the profile and
     * does not depend on the phone telling the truth, because it is computed
     * from the birth date and the server clock.
     */
    public boolean isMinorOn(LocalDate today) {
        return ageOn(today) < MIN_AGE;
    }

    /** The 1-to-5 scale, as the 0-to-1 value the algorithm needs. */
    public double sociabilityScore() {
        return (sociability - 1) / 4.0;
    }

    public double conversationDepthScore() {
        return (conversationDepth - 1) / 4.0;
    }
}
