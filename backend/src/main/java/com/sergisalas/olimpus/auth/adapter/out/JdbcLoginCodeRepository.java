package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.EmailAddress;
import com.sergisalas.olimpus.auth.domain.LoginCode;
import com.sergisalas.olimpus.auth.domain.LoginCodeRepository;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLoginCodeRepository implements LoginCodeRepository {

    private final JdbcTemplate jdbc;

    public JdbcLoginCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LoginCode> findByEmail(EmailAddress email) {
        return jdbc.query(
                        """
                        select email, code_hash, expires_at, attempts_left
                        from login_code where email = ?
                        """,
                        (rs, row) ->
                                new LoginCode(
                                        EmailAddress.of(rs.getString("email")),
                                        rs.getString("code_hash"),
                                        rs.getTimestamp("expires_at").toInstant(),
                                        rs.getInt("attempts_left")),
                        email.value())
                .stream()
                .findFirst();
    }

    @Override
    public void save(LoginCode code) {
        jdbc.update(
                """
                insert into login_code (email, code_hash, expires_at, attempts_left)
                values (?, ?, ?, ?)
                on conflict (email) do update set
                    code_hash = excluded.code_hash,
                    expires_at = excluded.expires_at,
                    attempts_left = excluded.attempts_left,
                    created_at = now()
                """,
                code.email().value(),
                code.codeHash(),
                Timestamp.from(code.expiresAt()),
                code.attemptsLeft());
    }

    @Override
    public void deleteByEmail(EmailAddress email) {
        jdbc.update("delete from login_code where email = ?", email.value());
    }
}
