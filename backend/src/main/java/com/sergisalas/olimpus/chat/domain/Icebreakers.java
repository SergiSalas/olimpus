package com.sergisalas.olimpus.chat.domain;

import com.sergisalas.olimpus.profile.domain.InterestCatalog;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * What the conversation's opening question is about.
 *
 * <p>The hardest moment in a chat app is the first message. Here nobody has to
 * come up with anything: the app asks a concrete question drawn from something
 * both people picked.
 *
 * <p>The <b>rarest</b> shared interest is chosen, not the first: both having
 * picked "travel" gives no conversation; both having picked "kendo" does.
 *
 * <p>The domain only chooses the interest. The question itself is worded by the
 * adapter in the reader's language.
 */
public final class Icebreakers {

    private Icebreakers() {}

    /**
     * The least common shared interest, or empty when they share none. Ties are
     * broken by name, so two people always get the same question.
     */
    public static Optional<String> rarestShared(Profile a, Profile b) {
        List<String> shared =
                a.interests().stream().filter(b.interests()::contains).sorted().toList();

        return shared.stream().min(Comparator.comparingDouble(InterestCatalog::popularityOf));
    }
}
