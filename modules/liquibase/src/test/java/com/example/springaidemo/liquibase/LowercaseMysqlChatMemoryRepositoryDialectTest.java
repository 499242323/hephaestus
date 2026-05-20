package com.example.springaidemo.liquibase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LowercaseMysqlChatMemoryRepositoryDialectTest {

    @Test
    void usesLowercaseChatMemoryTableNameInSql() {
        LowercaseMysqlChatMemoryRepositoryDialect dialect = new LowercaseMysqlChatMemoryRepositoryDialect();

        assertThat(dialect.getSelectMessagesSql()).contains("spring_ai_chat_memory");
        assertThat(dialect.getSelectMessagesSql()).contains("content, type, timestamp");
        assertThat(dialect.getInsertMessageSql()).contains("spring_ai_chat_memory");
        assertThat(dialect.getDeleteMessagesSql()).contains("spring_ai_chat_memory");
        assertThat(dialect.getSelectConversationIdsSql()).contains("spring_ai_chat_memory");
    }
}
