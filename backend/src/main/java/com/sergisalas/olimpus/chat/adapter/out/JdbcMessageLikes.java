package com.sergisalas.olimpus.chat.adapter.out;

import com.sergisalas.olimpus.chat.domain.MessageLikes;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMessageLikes implements MessageLikes {

    private final JdbcTemplate jdbc;

    public JdbcMessageLikes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void set(UUID messageId, Instant likedAt) {
        jdbc.update(
                "update message set liked_at = ? where id = ?",
                likedAt == null ? null : Timestamp.from(likedAt),
                messageId);
    }

    @Override
    public Set<UUID> likedIn(UUID conversationId) {
        return new HashSet<>(
                jdbc.queryForList(
                        "select id from message where conversation_id = ? and liked_at is not null",
                        UUID.class,
                        conversationId));
    }
}
