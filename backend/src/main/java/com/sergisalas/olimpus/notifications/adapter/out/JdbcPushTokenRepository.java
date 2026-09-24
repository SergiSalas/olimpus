package com.sergisalas.olimpus.notifications.adapter.out;

import com.sergisalas.olimpus.notifications.domain.PushTarget;
import com.sergisalas.olimpus.notifications.domain.PushTokenRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPushTokenRepository implements PushTokenRepository {

    private final JdbcTemplate jdbc;

    public JdbcPushTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(UUID accountId, String token, String language) {
        // A phone that changes hands brings its token to another account: the
        // token belongs to whoever logged in last.
        jdbc.update(
                """
                insert into push_token (token, account_id, language, updated_at)
                values (?, ?, ?, now())
                on conflict (token) do update set
                    account_id = excluded.account_id,
                    language = excluded.language,
                    updated_at = now()
                """,
                token,
                accountId,
                language);
    }

    @Override
    public List<PushTarget> tokensOf(UUID accountId) {
        return jdbc.query(
                "select token, language from push_token where account_id = ?",
                (rs, row) -> new PushTarget(rs.getString("token"), rs.getString("language")),
                accountId);
    }

    @Override
    public void delete(String token) {
        jdbc.update("delete from push_token where token = ?", token);
    }
}
