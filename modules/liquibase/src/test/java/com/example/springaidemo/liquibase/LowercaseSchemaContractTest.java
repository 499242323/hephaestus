package com.example.springaidemo.liquibase;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LowercaseSchemaContractTest {

    @Test
    void changelogUsesExpectedChatMemoryTableDefinition() throws Exception {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/changelog/db.changelog.xml");

        assertThat(stream).isNotNull();

        String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(xml).contains("spring_ai_chat_memory");
        assertThat(xml).contains("spring_ai_media_file");
        assertThat(xml).contains("column name=\"id\" type=\"int\" autoIncrement=\"true\"");
        assertThat(xml).contains("column name=\"create_time\" type=\"timestamp\"");
        assertThat(xml).contains("idx_conversation_id");
        assertThat(xml).contains("idx_create_time");
        assertThat(xml).contains("SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX");
    }
}
