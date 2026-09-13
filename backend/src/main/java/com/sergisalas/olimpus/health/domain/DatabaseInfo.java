package com.sergisalas.olimpus.health.domain;

import java.time.Instant;

/**
 * Outbound port: what the domain needs from the database, in its own words.
 * Whoever fulfils it (PostgreSQL today, a fake in tests) is the adapters'
 * business.
 */
public interface DatabaseInfo {

    String schemaVersion();

    Instant now();
}
