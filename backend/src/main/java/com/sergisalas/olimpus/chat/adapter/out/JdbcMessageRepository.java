package com.sergisalas.olimpus.chat.adapter.out;

import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMessageRepository implements MessageRepository {

    private final JdbcTemplate jdbc;

    public JdbcMessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Message message) {
        jdbc.update(
                """
                insert into message (id, conversation_id, sender, text, sent_at)
                values (?, ?, ?, ?, ?)
                """,
                message.id(),
                message.conversationId(),
                message.senderAccountId(),
                message.text(),
                Timestamp.from(message.sentAt()));
    }

    @Override
    public List<Message> byConversation(UUID conversationId) {
        return jdbc.query(
                """
                select id, conversation_id, sender, text, sent_at
                from message where conversation_id = ? order by sent_at, id
                """,
                (rs, row) ->
                        new Message(
                                rs.getObject("id", UUID.class),
                                rs.getObject("conversation_id", UUID.class),
                                rs.getObject("sender", UUID.class),
                                rs.getString("text"),
                                rs.getTimestamp("sent_at").toInstant()),
                conversationId);
    }
}
