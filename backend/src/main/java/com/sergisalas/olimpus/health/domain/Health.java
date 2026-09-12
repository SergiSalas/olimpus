package com.sergisalas.olimpus.health.domain;

import java.time.Instant;

/**
 * Lo que el sistema responde cuando se le pregunta si esta vivo.
 * Es una clase Java normal: no sabe nada de Spring ni de HTTP.
 */
public record Health(String schemaVersion, Instant databaseTime) {

    public Health {
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("falta la version del esquema");
        }
        if (databaseTime == null) {
            throw new IllegalArgumentException("falta la hora de la base de datos");
        }
    }
}
