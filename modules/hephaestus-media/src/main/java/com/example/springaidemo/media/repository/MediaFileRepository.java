package com.example.springaidemo.media.repository;

import com.example.springaidemo.media.domain.MediaFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class MediaFileRepository {

    private static final RowMapper<MediaFile> ROW_MAPPER = (rs, rowNum) -> new MediaFile(
            rs.getLong("id"),
            rs.getString("conversation_id"),
            rs.getString("original_filename"),
            rs.getString("stored_filename"),
            rs.getString("content_type"),
            rs.getLong("file_size"),
            rs.getString("storage_path"),
            rs.getString("access_url"),
            rs.getString("source_type"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public MediaFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MediaFile save(MediaFile mediaFile) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO spring_ai_media_file
                      (conversation_id, original_filename, stored_filename, content_type, file_size, storage_path, access_url, source_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, mediaFile.conversationId());
            ps.setString(2, mediaFile.originalFilename());
            ps.setString(3, mediaFile.storedFilename());
            ps.setString(4, mediaFile.contentType());
            ps.setLong(5, mediaFile.fileSize());
            ps.setString(6, mediaFile.storagePath());
            ps.setString(7, mediaFile.accessUrl());
            ps.setString(8, mediaFile.sourceType());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long id = key == null ? mediaFile.id() : key.longValue();
        return new MediaFile(
                id,
                mediaFile.conversationId(),
                mediaFile.originalFilename(),
                mediaFile.storedFilename(),
                mediaFile.contentType(),
                mediaFile.fileSize(),
                mediaFile.storagePath(),
                mediaFile.accessUrl(),
                mediaFile.sourceType(),
                mediaFile.createdAt()
        );
    }

    public Optional<MediaFile> findById(long id) {
        return jdbcTemplate.query("""
                SELECT id, conversation_id, original_filename, stored_filename, content_type, file_size,
                       storage_path, access_url, source_type, created_at
                FROM spring_ai_media_file
                WHERE id = ?
                """, ROW_MAPPER, id).stream().findFirst();
    }

    public void updateAccessUrl(long id, String accessUrl) {
        jdbcTemplate.update("UPDATE spring_ai_media_file SET access_url = ? WHERE id = ?", accessUrl, id);
    }
}
