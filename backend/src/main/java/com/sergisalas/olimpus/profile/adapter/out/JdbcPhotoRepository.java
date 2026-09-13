package com.sergisalas.olimpus.profile.adapter.out;

import com.sergisalas.olimpus.profile.domain.ModerationState;
import com.sergisalas.olimpus.profile.domain.Photo;
import com.sergisalas.olimpus.profile.domain.PhotoRepository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPhotoRepository implements PhotoRepository {

    private final JdbcTemplate jdbc;

    public JdbcPhotoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Photo> findByAccountId(UUID accountId) {
        return jdbc
                .query(
                        """
                        select account_id, storage_id, content_type, size_bytes, moderation, uploaded_at
                        from photo where account_id = ?
                        """,
                        (rs, row) ->
                                new Photo(
                                        rs.getObject("account_id", UUID.class),
                                        rs.getString("storage_id"),
                                        rs.getString("content_type"),
                                        rs.getLong("size_bytes"),
                                        ModerationState.valueOf(rs.getString("moderation")),
                                        rs.getTimestamp("uploaded_at").toInstant()),
                        accountId)
                .stream()
                .findFirst();
    }

    @Override
    public void save(Photo photo) {
        jdbc.update(
                """
                insert into photo (account_id, storage_id, content_type, size_bytes, moderation, uploaded_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (account_id) do update set
                    storage_id = excluded.storage_id,
                    content_type = excluded.content_type,
                    size_bytes = excluded.size_bytes,
                    moderation = excluded.moderation,
                    uploaded_at = excluded.uploaded_at
                """,
                photo.accountId(),
                photo.storageId(),
                photo.contentType(),
                photo.sizeBytes(),
                photo.moderation().name(),
                Timestamp.from(photo.uploadedAt()));
    }
}
