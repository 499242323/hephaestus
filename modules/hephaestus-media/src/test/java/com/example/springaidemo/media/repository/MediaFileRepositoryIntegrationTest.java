package com.example.springaidemo.media.repository;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.entity.MediaFileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test",
        "spring.ai.openai.base-url=http://localhost",
        "spring.datasource.url=jdbc:h2:mem:hephaestus_media;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class MediaFileRepositoryIntegrationTest {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS spring_ai_media_file (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    conversation_id VARCHAR(128) NOT NULL,
                    original_filename VARCHAR(255) NOT NULL,
                    stored_filename VARCHAR(255) NOT NULL,
                    content_type VARCHAR(128) NOT NULL,
                    file_size BIGINT NOT NULL,
                    storage_path VARCHAR(512) NOT NULL,
                    access_url VARCHAR(512) NOT NULL,
                    source_type VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.update("DELETE FROM spring_ai_media_file");
    }

    @Test
    void savesFindsUpdatesAndBatchInsertsMediaFiles() {
        MediaFile saved = mediaFileRepository.saveMediaFile(new MediaFile(
                null,
                "session-1",
                "a.txt",
                "a-txt",
                "text/plain",
                10L,
                "rec/upload/a-txt",
                "",
                "USER_UPLOAD",
                LocalDateTime.now()
        ));

        assertThat(saved.id()).isNotNull();

        MediaFile loaded = mediaFileRepository.findById(saved.id()).orElseThrow();
        assertThat(loaded.originalFilename()).isEqualTo("a.txt");

        mediaFileRepository.updateAccessUrl(saved.id(), "/hephaestus/media/a-txt");
        MediaFile updated = mediaFileRepository.findById(saved.id()).orElseThrow();
        assertThat(updated.accessUrl()).isEqualTo("/hephaestus/media/a-txt");

        MediaFile replaced = mediaFileRepository.updateMediaFile(new MediaFile(
                saved.id(),
                saved.conversationId(),
                saved.originalFilename(),
                saved.storedFilename(),
                saved.contentType(),
                saved.fileSize(),
                saved.storagePath(),
                "/hephaestus/media/a-txt-2",
                saved.sourceType(),
                saved.createdAt()
        ));
        assertThat(replaced.id()).isEqualTo(saved.id());
        assertThat(mediaFileRepository.findById(saved.id()).orElseThrow().accessUrl())
                .isEqualTo("/hephaestus/media/a-txt-2");

        MediaFileEntity first = MediaFileEntity.fromDomain(new MediaFile(
                null,
                "session-2",
                "b.txt",
                "b-txt",
                "text/plain",
                11L,
                "rec/upload/b-txt",
                "",
                "USER_UPLOAD",
                LocalDateTime.now()
        ));
        MediaFileEntity second = MediaFileEntity.fromDomain(new MediaFile(
                null,
                "session-3",
                "c.txt",
                "c-txt",
                "text/plain",
                12L,
                "rec/upload/c-txt",
                "",
                "USER_UPLOAD",
                LocalDateTime.now()
        ));

        mediaFileRepository.insertList(List.of(first, second));

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();

        mediaFileRepository.deleteMediaFile(saved.id());
        assertThat(mediaFileRepository.findById(saved.id())).isEmpty();
    }
}
