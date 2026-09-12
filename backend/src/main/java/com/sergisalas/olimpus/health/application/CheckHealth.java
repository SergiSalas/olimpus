package com.sergisalas.olimpus.health.application;

import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import com.sergisalas.olimpus.health.domain.Health;

/**
 * Caso de uso: comprobar que la aplicacion y su base de datos se hablan.
 * Sin anotaciones: se construye a mano en {@code config.DomainBeans}.
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
