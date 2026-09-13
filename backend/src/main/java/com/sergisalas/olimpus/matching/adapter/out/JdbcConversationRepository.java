package com.sergisalas.olimpus.matching.adapter.out;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.ConversationState;
import com.sergisalas.olimpus.matching.domain.Origin;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcConversationRepository implements ConversationRepository {

    private static final String COLUMNS =
            """
            id, round_date, round_kind, account_a, account_b, origin, score,
            opens_at, closes_at, state, messages_from_a, messages_from_b, icebreaker_interest
            """;

    private static final RowMapper<Conversation> MAPPER = JdbcConversationRepository::toConversation;

    private final JdbcTemplate jdbc;

    public JdbcConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Conversation c) {
        jdbc.update(
                """
                insert into conversation (
                    id, round_date, round_kind, account_a, account_b, origin, score,
                    opens_at, closes_at, state, messages_from_a, messages_from_b, icebreaker_interest)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    state = excluded.state,
                    messages_from_a = excluded.messages_from_a,
                    messages_from_b = excluded.messages_from_b
                """,
                c.id(),
                java.sql.Date.valueOf(c.roundDate()),
                c.roundKind().name(),
                c.accountA(),
                c.accountB(),
                c.origin().name(),
                c.score(),
                Timestamp.from(c.opensAt()),
                Timestamp.from(c.closesAt()),
                c.state().name(),
                c.messagesFromA(),
                c.messagesFromB(),
                c.icebreakerInterest());
    }

    @Override
    public List<Conversation> byDate(LocalDate date) {
        return jdbc.query(
                "select " + COLUMNS + " from conversation where round_date = ? order by created_at",
                MAPPER,
                java.sql.Date.valueOf(date));
    }

    @Override
    public Optional<Conversation> openFor(UUID accountId, LocalDate date) {
        return jdbc.query(
                        "select "
                                + COLUMNS
                                + """
                                from conversation
                                where round_date = ?
                                  and state = 'OPEN'
                                  and (account_a = ? or account_b = ?)
                                order by created_at desc
                                limit 1
                                """,
                        MAPPER,
                        java.sql.Date.valueOf(date),
                        accountId,
                        accountId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Conversation> byId(UUID id) {
        return jdbc.query("select " + COLUMNS + " from conversation where id = ?", MAPPER, id)
                .stream()
                .findFirst();
    }

    private static Conversation toConversation(ResultSet rs, int row) throws SQLException {
        return new Conversation(
                rs.getObject("id", UUID.class),
                rs.getDate("round_date").toLocalDate(),
                RoundKind.valueOf(rs.getString("round_kind")),
                rs.getObject("account_a", UUID.class),
                rs.getObject("account_b", UUID.class),
                Origin.valueOf(rs.getString("origin")),
                rs.getDouble("score"),
                rs.getTimestamp("opens_at").toInstant(),
                rs.getTimestamp("closes_at").toInstant(),
                ConversationState.valueOf(rs.getString("state")),
                rs.getInt("messages_from_a"),
                rs.getInt("messages_from_b"),
                rs.getString("icebreaker_interest"));
    }
}
