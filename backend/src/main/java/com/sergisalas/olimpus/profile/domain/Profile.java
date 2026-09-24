package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What someone declares at sign-up.
 *
 * <p>It is only the starting point. Over time, how they actually behave (who
 * they keep talking to, who they connect with) should weigh more than what they
 * said on day one.
 *
 * <p>The rules live here, in the constructor: a profile that exists is a valid
 * profile. Nobody can store a half-filled one through another door.
 *
 * <p>Two of the fields are declared in two pieces on purpose. Gender has the
 * word the person uses ({@code genderLabel}, shown, free) and the box that
 * matching works with ({@code gender}, four values, a hard filter): being
 * precise about yourself should not turn the filter into a minefield. And what
 * used to be a free bio is now {@code prompts}, answers to set questions,
 * because an empty box gets filled with a shrug.
 */
public record Profile(
        UUID accountId,
        String nickname,
        LocalDate birthDate,
        Gender gender,
        /** How the person says it. Shown at level 3, never used to match. */
        String genderLabel,
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
        Set<String> interests,
        /** Three answered questions. This is what opens at level 2. */
        List<PromptAnswer> prompts,
        /** Level 3, optional. */
        String occupation,
        /** Level 3, optional. */
        String fromPlace) {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 99;
    public static final int MIN_INTERESTS = 5;
    public static final int MAX_INTERESTS = 8;
    public static final int MAX_LANGUAGES = 5;
    public static final int MIN_NICKNAME = 2;
    public static final int MAX_NICKNAME = 20;

    /** Exactly three, like the questions the sign-up asks. Not two, not four. */
    public static final int PROMPTS = 3;

    public static final int MAX_GENDER_LABEL = 40;
    public static final int MAX_OCCUPATION = 60;
    public static final int MAX_FROM_PLACE = 60;

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

        if (birthDate == null) throw new RuleViolationException("profile.birth-date.missing");
        if (gender == null) throw new RuleViolationException("profile.gender.missing");

        genderLabel = trimmed(genderLabel, MAX_GENDER_LABEL, "profile.gender-label.too-long");

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

        if (prompts == null || prompts.size() != PROMPTS) {
            throw new RuleViolationException("profile.prompts.count", PROMPTS);
        }
        prompts = List.copyOf(prompts);
        // Answering the same question three times would leave level 2 as empty
        // as the bio this replaced.
        if (prompts.stream().map(PromptAnswer::question).distinct().count() != prompts.size()) {
            throw new RuleViolationException("profile.prompts.duplicate");
        }

        occupation = trimmed(occupation, MAX_OCCUPATION, "profile.occupation.too-long");
        fromPlace = trimmed(fromPlace, MAX_FROM_PLACE, "profile.from-place.too-long");
    }

    /** The optional bits of text: never null once stored, empty when left out. */
    private static String trimmed(String value, int max, String errorKey) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() > max) throw new RuleViolationException(errorKey, max);
        return clean;
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
