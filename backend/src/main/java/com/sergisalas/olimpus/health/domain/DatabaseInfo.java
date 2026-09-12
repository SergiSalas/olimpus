package com.sergisalas.olimpus.health.domain;

import java.time.Instant;

/**
 * Puerto de salida: lo que el dominio necesita de la base de datos, dicho
 * con sus palabras. Quien lo cumpla (PostgreSQL hoy, una version falsa en
 * los tests) es asunto de los adaptadores.
 */
public interface DatabaseInfo {

    String schemaVersion();

    Instant now();
}
