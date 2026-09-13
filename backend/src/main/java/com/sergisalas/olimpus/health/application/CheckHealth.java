package com.sergisalas.olimpus.health.application;

import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import com.sergisalas.olimpus.health.domain.Health;

/**
 * Use case: check that the application and its database can talk to each other.
 * No annotations: it is built by hand in {@code config.DomainBeans}.
 */
public class CheckHealth {

    private final DatabaseInfo databaseInfo;

    public CheckHealth(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    public Health execute() {
        return new Health(databaseInfo.schemaVersion(), databaseInfo.now());
    }
}
