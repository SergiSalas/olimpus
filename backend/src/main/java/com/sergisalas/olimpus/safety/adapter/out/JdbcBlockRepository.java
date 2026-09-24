package com.sergisalas.olimpus.safety.adapter.out;

import com.sergisalas.olimpus.safety.domain.Block;
import com.sergisalas.olimpus.safety.domain.BlockRepository;
import com.sergisalas.olimpus.safety.domain.ReportReason;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBlockRepository implements BlockRepository {

    private final JdbcTemplate jdbc;

    public JdbcBlockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Block block) {
        // Blocking twice is not an error, and a later report must not erase the
        // reason of an earlier one.
        jdbc.update(
                """
                insert into block (blocker, blocked, reason, created_at)
                values (?, ?, ?, ?)
                on conflict (blocker, blocked) do update set
                    reason = coalesce(excluded.reason, block.reason)
                """,
                block.blocker(),
                block.blocked(),
                block.reason() == null ? null : block.reason().name(),
                Timestamp.from(block.createdAt()));
    }

    @Override
    public List<Block> byBlocker(UUID blocker) {
        return jdbc.query(
                "select blocker, blocked, reason, created_at from block where blocker = ?",
                (rs, row) -> {
                    String reason = rs.getString("reason");
                    return new Block(
                            rs.getObject("blocker", UUID.class),
                            rs.getObject("blocked", UUID.class),
                            reason == null ? null : ReportReason.valueOf(reason),
                            rs.getTimestamp("created_at").toInstant());
                },
                blocker);
    }
}
