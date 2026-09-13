package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The little that is visible of the other person at the start: level 0 of the
 * ladder.
 *
 * <p>Age, two interests and how far away they are. No nickname, no bio, no
 * photo: those come when the conversation earns them.
 *
 * <p>Having this as a separate object, instead of the profile trimmed on the
 * screen, is deliberate: there is no way a slip sends the whole profile to the
 * phone. Whatever is not here never leaves the server.
 */
public record PartnerView(int age, List<String> interestsShown, int approxDistanceKm, int level) {

    public static final int INTERESTS_VISIBLE_AT_START = 2;

    /**
     * @param shownFirst interests worth showing first, usually the ones both
     *     have in common: they give something to talk about
     */
    public static PartnerView levelZero(
            Profile partner, Profile viewer, LocalDate today, List<String> shownFirst) {

        List<String> visible = new ArrayList<>();
        for (String interest : shownFirst) {
            if (partner.interests().contains(interest) && visible.size() < INTERESTS_VISIBLE_AT_START) {
                visible.add(interest);
            }
        }
        for (String interest : partner.interests()) {
            if (visible.size() >= INTERESTS_VISIBLE_AT_START) break;
            if (!visible.contains(interest)) visible.add(interest);
        }

        int km = (int) Math.round(viewer.location().distanceKmTo(partner.location()));

        return new PartnerView(partner.ageOn(today), List.copyOf(visible), km, 0);
    }

    /** Interests both have, which are the ones that help get started. */
    public static List<String> sharedInterests(Profile a, Profile b) {
        return a.interests().stream().filter(b.interests()::contains).sorted().toList();
    }
}
