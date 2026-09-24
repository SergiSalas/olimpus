package com.sergisalas.olimpus.notifications.adapter.out;

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
    public void save(UUID accountId, String token) {
        // A phone that changes hands brings its token to another account: the
        // token belongs to whoever logged in last.
        jdbc.update(
                """
                insert into push_token (token, account_id, updated_at)
                values (?, ?, now())
                on conflict (token) do update set
                    account_id = excluded.account_id,
                    updated_at = now()
                """,
                token,
                accountId);
    }

    @Override
    public List<String> tokensOf(UUID accountId) {
        return jdbc.queryForList(
                "select token from push_token where account_id = ?", String.class, accountId);
    }

    @Override
    public void delete(String token) {
        jdbc.update("delete from push_token where token = ?", token);
    }
}
