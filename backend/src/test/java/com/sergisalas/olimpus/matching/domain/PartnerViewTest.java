package com.sergisalas.olimpus.matching.domain;

import static com.sergisalas.olimpus.matching.domain.TestPeople.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What each level shows. These tests are the real guard on the whole idea of the
 * app: if one of them starts failing, something is being revealed before it has
 * been earned.
 */
class PartnerViewTest {

    private final Profile leo =
            TestPeople.person()
                    .nickname("Leo")
                    .age(33)
                    .interests("climbing", "movies", "wine", "chess", "reading")
                    .build();

    private final Profile ana =
            TestPeople.person()
                    .nickname("Ana")
                    .age(31)
                    .interests("climbing", "movies", "running", "art", "cooking")
                    .build();

    private final List<String> shared = PartnerView.sharedInterests(ana, leo);

    private PartnerView at(UnlockLevel level) {
        return PartnerView.at(level, leo, ana, TODAY, shared);
    }

    @Test
    void level_zero_shows_age_two_interests_and_the_distance() {
        PartnerView view = at(UnlockLevel.MATCH);

        assertThat(view.age()).isEqualTo(33);
        assertThat(view.interestsShown()).hasSize(2);
        assertThat(view.approxDistanceKm()).isNotNegative();
    }

    @Test
    void level_zero_hides_the_nickname_the_bio_and_the_photo() {
        PartnerView view = at(UnlockLevel.MATCH);

        assertThat(view.nickname()).isNull();
        assertThat(view.bio()).isNull();
        assertThat(view.languages()).isEmpty();
        assertThat(view.intent()).isNull();
        assertThat(view.photoAvailable()).isFalse();
    }

    @Test
    void the_two_interests_shown_first_are_the_shared_ones() {
        assertThat(at(UnlockLevel.MATCH).interestsShown()).containsExactlyInAnyOrder("climbing", "movies");
    }

    @Test
    void level_one_adds_the_nickname_and_every_interest() {
        PartnerView view = at(UnlockLevel.FIRST_MESSAGE);

        assertThat(view.nickname()).isEqualTo("Leo");
        assertThat(view.interestsShown()).hasSize(5);
        assertThat(view.bio()).isNull();
        assertThat(view.photoAvailable()).isFalse();
    }

    @Test
    void level_two_adds_the_bio_and_still_no_photo() {
        PartnerView view = at(UnlockLevel.CONVERSATION);

        assertThat(view.bio()).isNotNull();
        assertThat(view.photoAvailable()).isFalse();
        assertThat(view.languages()).isEmpty();
    }

    @Test
    void level_three_opens_the_photo_and_the_wider_profile() {
        PartnerView view = at(UnlockLevel.GOOD_CONNECTION);

        assertThat(view.photoAvailable()).isTrue();
        assertThat(view.languages()).isNotEmpty();
        assertThat(view.intent()).isNotNull();
        assertThat(view.nickname()).isEqualTo("Leo");
        assertThat(view.bio()).isNotNull();
    }

    @Test
    void the_view_carries_no_photo_bytes_at_any_level() {
        // The photo is asked for separately, and that request is checked again.
        var fields =
                java.util.Arrays.stream(PartnerView.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList();

        assertThat(fields)
                .containsExactly(
                        "level",
                        "age",
                        "interestsShown",
                        "approxDistanceKm",
                        "nickname",
                        "bio",
                        "languages",
                        "intent",
                        "photoAvailable");
    }
}
