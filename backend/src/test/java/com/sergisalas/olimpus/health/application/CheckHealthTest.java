package com.sergisalas.olimpus.health.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import com.sergisalas.olimpus.health.domain.Health;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests the use case without Spring, without a database and without network: in
 * milliseconds. That is the upside of an isolated domain.
 */
class CheckHealthTest {

    private static final Instant SOME_TIME = Instant.parse("2026-09-12T04:00:00Z");

    @Test
    void reports_the_schema_version_and_the_database_time() {
        CheckHealth checkHealth = new CheckHealth(new FakeDatabaseInfo("v1", SOME_TIME));

        Health health = checkHealth.execute();

        assertThat(health.schemaVersion()).isEqualTo("v1");
        assertThat(health.databaseTime()).isEqualTo(SOME_TIME);
    }

    @Test
    void refuses_to_build_a_health_without_a_version() {
        CheckHealth checkHealth = new CheckHealth(new FakeDatabaseInfo("  ", SOME_TIME));

        assertThatThrownBy(checkHealth::execute)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schema version");
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
