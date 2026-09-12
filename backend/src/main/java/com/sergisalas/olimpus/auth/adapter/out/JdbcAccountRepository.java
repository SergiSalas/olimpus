package com.sergisalas.olimpus.auth.adapter.out;

import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.auth.domain.AccountRepository;
import com.sergisalas.olimpus.auth.domain.EmailAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountRepository implements AccountRepository {

    private static final RowMapper<Account> MAPPER = JdbcAccountRepository::toAccount;

    private final JdbcTemplate jdbc;

    public JdbcAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        return jdbc.query(
                        "select id, email, created_at from account where email = ?",
                        MAPPER,
                        email.value())
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jdbc.query("select id, email, created_at from account where id = ?", MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public Account save(Account account) {
        jdbc.update(
                """
                insert into account (id, email, created_at) values (?, ?, ?)
                on conflict (email) do nothing
                """,
                account.id(),
                account.email().value(),
                java.sql.Timestamp.from(account.createdAt()));
        return findByEmail(account.email()).orElse(account);
    }

    private static Account toAccount(ResultSet rs, int row) throws SQLException {
        return new Account(
                rs.getObject("id", UUID.class),
                EmailAddress.of(rs.getString("email")),
                rs.getTimestamp("created_at").toInstant());
    }
}
