package com.sergisalas.olimpus.profile.domain;

import static com.sergisalas.olimpus.profile.domain.TestProfiles.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileTest {

    @Test
    void a_complete_profile_builds_without_complaints() {
        assertThat(TestProfiles.valid().build().nickname()).isEqualTo("Sergi");
    }

    @Test
    void age_comes_from_the_birth_date() {
        Profile sergi = TestProfiles.valid().birthDate(LocalDate.of(1995, 3, 20)).build();

        assertThat(sergi.ageOn(TODAY)).isEqualTo(31);
        assertThat(sergi.isMinorOn(TODAY)).isFalse();
    }

    @Test
    void the_day_before_turning_eighteen_is_still_a_minor() {
        Profile almost = TestProfiles.valid().birthDate(TODAY.minusYears(18).plusDays(1)).build();
        Profile exactly = TestProfiles.valid().birthDate(TODAY.minusYears(18)).build();

        assertThat(almost.isMinorOn(TODAY)).isTrue();
        assertThat(exactly.isMinorOn(TODAY)).isFalse();
    }

    @Test
    void the_one_to_five_scales_become_zero_to_one_for_the_algorithm() {
        assertThat(TestProfiles.valid().sociability(1).build().sociabilityScore()).isZero();
        assertThat(TestProfiles.valid().sociability(3).build().sociabilityScore()).isEqualTo(0.5);
        assertThat(TestProfiles.valid().sociability(5).build().sociabilityScore()).isEqualTo(1.0);
        assertThat(TestProfiles.valid().conversationDepth(5).build().conversationDepthScore())
                .isEqualTo(1.0);
    }

    @Test
    void the_nickname_is_trimmed() {
        assertThat(TestProfiles.valid().nickname("  Ana  ").build().nickname()).isEqualTo("Ana");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "A", "anexaggeratedlylongnicknamereally"})
    void a_nickname_that_is_too_short_or_too_long_is_rejected(String nickname) {
        assertThatThrownBy(() -> TestProfiles.valid().nickname(nickname).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.nickname.length");
    }

    @Test
    void the_bio_has_a_limit() {
        assertThatThrownBy(() -> TestProfiles.valid().bio("x".repeat(201)).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.bio.too-long");
    }

    @Test
    void at_least_one_gender_must_be_sought() {
        assertThatThrownBy(() -> TestProfiles.valid().seeking(Set.of()).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.seeking.empty");
    }

    @Test
    void the_age_range_cannot_be_reversed() {
        assertThatThrownBy(() -> TestProfiles.valid().ages(40, 25).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.age-range.reversed");
    }

    @Test
    void nobody_can_look_for_minors() {
        assertThatThrownBy(() -> TestProfiles.valid().ages(16, 30).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.age-range.out-of-bounds");
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 30, 65, 120})
    void distance_goes_from_five_to_one_hundred_twenty_in_steps_of_five(int km) {
        assertThat(TestProfiles.valid().maxDistanceKm(km).build().maxDistanceKm()).isEqualTo(km);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 7, 125, 1000})
    void a_distance_out_of_range_or_off_step_is_rejected(int km) {
        assertThatThrownBy(() -> TestProfiles.valid().maxDistanceKm(km).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.distance.invalid");
    }

    @Test
    void between_five_and_eight_interests_are_required() {
        assertThatThrownBy(() -> TestProfiles.valid().interests(Set.of("movies", "reading")).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.interests.count");

        assertThatThrownBy(
                        () ->
                                TestProfiles.valid()
                                        .interests(
                                                Set.of(
                                                        "movies", "reading", "running", "surfing", "wine",
                                                        "theatre", "art", "diving", "kendo"))
                                        .build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.interests.count");
    }

    @Test
    void a_made_up_interest_is_rejected() {
        assertThatThrownBy(
                        () ->
                                TestProfiles.valid()
                                        .interests(
                                                Set.of("movies", "reading", "running", "surfing", "moon-bungee"))
                                        .build())
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("profile.interests.unknown")
                .hasMessageContaining("moon-bungee");
    }

    @Test
    void at_least_one_language_is_required_and_without_repeats() {
        assertThatThrownBy(() -> TestProfiles.valid().languages(List.of()).build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.languages.empty");

        assertThatThrownBy(
                        () ->
                                TestProfiles.valid()
                                        .languages(
                                                List.of(
                                                        new LanguageSkill("es", LanguageSkill.Level.NATIVE),
                                                        new LanguageSkill("es", LanguageSkill.Level.BASIC)))
                                        .build())
                .isInstanceOf(RuleViolationException.class)
                .extracting("messageKey")
                .isEqualTo("profile.languages.duplicate");
    }

    @Test
    void the_interest_catalog_is_the_one_from_the_lab() {
        assertThat(InterestCatalog.size()).isEqualTo(38);
        assertThat(InterestCatalog.contains("ice-climbing")).isTrue();
        assertThat(InterestCatalog.contains("travel")).isTrue();
        assertThat(InterestCatalog.contains("whatever")).isFalse();
    }
}
