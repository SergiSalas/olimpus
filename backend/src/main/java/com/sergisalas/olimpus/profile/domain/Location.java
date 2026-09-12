package com.sergisalas.olimpus.profile.domain;

/**
 * Donde esta alguien, a proposito con poca precision.
 *
 * <p>Se guarda redondeado a dos decimales, algo mas de un kilometro. La
 * distancia sigue sirviendo para emparejar y, si algun dia se filtraran los
 * datos, no senalan la casa de nadie.
 */
public record Location(double latitude, double longitude) {

    private static final double RADIO_TIERRA_KM = 6371.0;

    public Location {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitud fuera de rango");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitud fuera de rango");
        }
    }

    /** Unica forma de crear una ubicacion desde el GPS: siempre redondeada. */
    public static Location rounded(double latitude, double longitude) {
        return new Location(redondear(latitude), redondear(longitude));
    }

    private static double redondear(double grados) {
        return Math.round(grados * 100.0) / 100.0;
    }

    /** Distancia en kilometros por el camino corto sobre la esfera. */
    public double distanceKmTo(Location other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(latitude))
                                * Math.cos(Math.toRadians(other.latitude))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
