package com.sergisalas.olimpus.health.domain;

import java.time.Instant;

/**
 * What the system answers when asked whether it is alive.
 * It is a plain Java class: it knows nothing about Spring or HTTP.
 */
public record Health(String schemaVersion, Instant databaseTime) {

    public Health {
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schema version is missing");
        }
        if (databaseTime == null) {
            throw new IllegalArgumentException("database time is missing");
        }
    }
}
