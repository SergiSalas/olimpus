package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.Session;
import com.sergisalas.olimpus.auth.domain.SessionRepository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSessionRepository implements SessionRepository {

    private final JdbcTemplate jdbc;

    public JdbcSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Session> findByTokenHash(String tokenHash) {
        return jdbc.query(
                        """
                        select token_hash, account_id, created_at, expires_at, revoked_at
                        from session where token_hash = ?
                        """,
                        (rs, row) -> {
                            Timestamp revoked = rs.getTimestamp("revoked_at");
                            return new Session(
                                    rs.getString("token_hash"),
                                    rs.getObject("account_id", UUID.class),
                                    rs.getTimestamp("created_at").toInstant(),
                                    rs.getTimestamp("expires_at").toInstant(),
                                    revoked == null ? null : revoked.toInstant());
                        },
                        tokenHash)
                .stream()
                .findFirst();
    }

    @Override
    public void save(Session session) {
        jdbc.update(
                """
                insert into session (token_hash, account_id, created_at, expires_at, revoked_at)
                values (?, ?, ?, ?, ?)
                on conflict (token_hash) do update set revoked_at = excluded.revoked_at
                """,
                session.tokenHash(),
                session.accountId(),
                Timestamp.from(session.createdAt()),
                Timestamp.from(session.expiresAt()),
                session.revokedAt() == null ? null : Timestamp.from(session.revokedAt()));
    }
}
