package com.sergisalas.olimpus.health.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import com.sergisalas.olimpus.health.domain.Health;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Prueba del caso de uso sin Spring, sin base de datos y sin red: en
 * milisegundos. Es la ventaja de tener el dominio aislado.
 */
class CheckHealthTest {

    private static final Instant UNA_HORA = Instant.parse("2026-09-12T04:00:00Z");

    @Test
    void informa_de_la_version_del_esquema_y_de_la_hora_de_la_base_de_datos() {
        CheckHealth checkHealth = new CheckHealth(new FakeDatabaseInfo("v1", UNA_HORA));

        Health health = checkHealth.execute();

        assertThat(health.schemaVersion()).isEqualTo("v1");
        assertThat(health.databaseTime()).isEqualTo(UNA_HORA);
    }

    @Test
    void se_niega_a_construir_un_estado_sin_version() {
        CheckHealth checkHealth = new CheckHealth(new FakeDatabaseInfo("  ", UNA_HORA));

        assertThatThrownBy(checkHealth::execute)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version del esquema");
    }

    private record FakeDatabaseInfo(String version, Instant instant) implements DatabaseInfo {
        @Override
        public String schemaVersion() {
            return version;
        }

        @Override
        public Instant now() {
            return instant;
        }
    }
}
