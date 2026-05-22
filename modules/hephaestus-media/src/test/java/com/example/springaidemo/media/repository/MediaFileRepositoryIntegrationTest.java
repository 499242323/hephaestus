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
        "spring.ai.openai.base-url=http://localhost"
})
class MediaFileRepositoryIntegrationTest {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTable() {
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
    }
}
