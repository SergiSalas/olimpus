package com.sergisalas.olimpus.health.adapter.out;

import com.sergisalas.olimpus.health.domain.DatabaseInfo;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Outbound adapter: fulfils the port by asking PostgreSQL. */
@Repository
public class JdbcDatabaseInfo implements DatabaseInfo {

    private final JdbcTemplate jdbc;

    public JdbcDatabaseInfo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String schemaVersion() {
        return jdbc.queryForObject(
                "select value from app_info where name = 'schema'", String.class);
    }

    @Override
    public Instant now() {
        OffsetDateTime now = jdbc.queryForObject("select now()", OffsetDateTime.class);
        return now.toInstant();
    }
}
