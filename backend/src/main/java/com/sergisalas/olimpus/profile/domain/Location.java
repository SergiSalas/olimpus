package com.sergisalas.olimpus.profile.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;

/**
 * Where someone is, deliberately imprecise.
 *
 * <p>It is stored rounded to two decimals, a bit over a kilometre. The distance
 * is still useful for matching and, if the data ever leaked, it would not point
 * at anyone's home.
 */
public record Location(double latitude, double longitude) {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public Location {
        if (latitude < -90 || latitude > 90) {
            throw new RuleViolationException("location.latitude.out-of-range");
        }
        if (longitude < -180 || longitude > 180) {
            throw new RuleViolationException("location.longitude.out-of-range");
        }
    }

    /** The only way to build a location from GPS: always rounded. */
    public static Location rounded(double latitude, double longitude) {
        return new Location(round(latitude), round(longitude));
    }

    private static double round(double degrees) {
        return Math.round(degrees * 100.0) / 100.0;
    }

    /** Great-circle distance in kilometres. */
    public double distanceKmTo(Location other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(latitude))
                                * Math.cos(Math.toRadians(other.latitude))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
