package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Intent;
import com.sergisalas.olimpus.profile.domain.Profile;
import com.sergisalas.olimpus.profile.domain.PromptAnswer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * What is visible of the other person, given the level the conversation has
 * earned.
 *
 * <p>Having this as a separate object, instead of the profile trimmed on the
 * screen, is deliberate: there is no way a slip sends the whole profile to the
 * phone. <b>Whatever is not here never leaves the server</b>, and a field only
 * gets filled in when its level says so.
 *
 * <p>The photo is not here on purpose either, not even at level 3: its bytes are
 * asked for separately, and that request is checked again.
 */
public record PartnerView(
        int level,
        int age,
        List<String> interestsShown,
        int approxDistanceKm,
        /** Level 1 and up. Null below. */
        String nickname,
        /** Level 2 and up: the three answered questions. Empty below. */
        List<PromptAnswer> prompts,
        /** Level 3: the wider profile. Empty below. */
        List<String> languages,
        /** Level 3. Null below. */
        Intent intent,
        /** Level 3. Null below, and empty when the person did not say it. */
        String genderLabel,
        /** Level 3. Null below. */
        String occupation,
        /** Level 3. Null below. */
        String fromPlace,
        /** Level 3: the photo can now be asked for. */
        boolean photoAvailable) {

    public static final int INTERESTS_VISIBLE_AT_START = 2;

    /**
     * @param shownFirst interests worth showing first, usually the ones both
     *     have in common: they give something to talk about
     */
    public static PartnerView at(
            UnlockLevel level,
            Profile partner,
            Profile viewer,
            LocalDate today,
            List<String> shownFirst) {

        boolean firstMessage = level.atLeast(UnlockLevel.FIRST_MESSAGE);
        boolean conversation = level.atLeast(UnlockLevel.CONVERSATION);
        boolean goodConnection = level.atLeast(UnlockLevel.GOOD_CONNECTION);

        List<String> interests =
                firstMessage
                        ? partner.interests().stream().sorted().toList()
                        : firstFew(partner, shownFirst);

        return new PartnerView(
                level.number(),
                partner.ageOn(today),
                interests,
                (int) Math.round(viewer.location().distanceKmTo(partner.location())),
                firstMessage ? partner.nickname() : null,
                conversation ? partner.prompts() : List.of(),
                goodConnection
                        ? partner.languages().stream().map(l -> l.code()).sorted().toList()
                        : List.of(),
                goodConnection ? partner.intent() : null,
                goodConnection ? partner.genderLabel() : null,
                goodConnection ? partner.occupation() : null,
                goodConnection ? partner.fromPlace() : null,
                goodConnection);
    }

    /** Level 0: only two interests, the shared ones first. */
    private static List<String> firstFew(Profile partner, List<String> shownFirst) {
        List<String> visible = new ArrayList<>();
        for (String interest : shownFirst) {
            if (partner.interests().contains(interest)
                    && visible.size() < INTERESTS_VISIBLE_AT_START) {
                visible.add(interest);
            }
        }
        for (String interest : partner.interests().stream().sorted().toList()) {
            if (visible.size() >= INTERESTS_VISIBLE_AT_START) break;
            if (!visible.contains(interest)) visible.add(interest);
        }
        return List.copyOf(visible);
    }

    /** Interests both have, which are the ones that help get started. */
    public static List<String> sharedInterests(Profile a, Profile b) {
        return a.interests().stream().filter(b.interests()::contains).sorted().toList();
    }
}
