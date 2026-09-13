package com.sergisalas.olimpus.profile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void the_location_is_stored_rounded_to_two_decimals() {
        Location exact = Location.rounded(41.387423, 2.168665);

        assertThat(exact.latitude()).isEqualTo(41.39);
        assertThat(exact.longitude()).isEqualTo(2.17);
    }

    @Test
    void rounding_loses_at_most_a_bit_over_a_kilometre() {
        Location exact = new Location(41.387423, 2.168665);
        Location stored = Location.rounded(41.387423, 2.168665);

        assertThat(exact.distanceKmTo(stored)).isLessThan(1.6);
    }

    @Test
    void two_points_in_the_same_city_are_a_few_kilometres_apart() {
        Location sagradaFamilia = Location.rounded(41.4036, 2.1744);
        Location barceloneta = Location.rounded(41.3797, 2.1900);

        assertThat(sagradaFamilia.distanceKmTo(barceloneta)).isBetween(2.0, 4.0);
    }

    @Test
    void barcelona_and_madrid_are_about_five_hundred_kilometres_apart() {
        Location barcelona = Location.rounded(41.3874, 2.1686);
        Location madrid = Location.rounded(40.4168, -3.7038);

        assertThat(barcelona.distanceKmTo(madrid)).isBetween(500.0, 520.0);
    }

    @Test
    void the_distance_from_a_point_to_itself_is_zero() {
        Location here = Location.rounded(41.3874, 2.1686);

        assertThat(here.distanceKmTo(here)).isZero();
    }

    @Test
    void impossible_coordinates_are_rejected() {
        assertThatThrownBy(() -> new Location(95, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Location(0, 200)).isInstanceOf(IllegalArgumentException.class);
    }
}
