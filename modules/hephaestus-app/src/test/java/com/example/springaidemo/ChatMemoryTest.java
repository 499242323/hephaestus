package com.example.springaidemo;

import com.example.springaidemo.config.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.ai.openai.chat.options.stream-usage=false"
})
class ChatMemoryTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseCreatesLowercaseChatMemoryTable() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                        """,
                Integer.class,
                "spring_ai_chat_memory");

        assertThat(count).isNotNull();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void testMultiTurnConversation() {
        String conversationId = "test_user_001_" + System.currentTimeMillis();

        String response1 = chatService.chat("My name is Zhang San and I am a Java engineer.", conversationId);
        System.out.println("AI: " + response1);

        String response2 = chatService.chat("What is my name and what job do I do?", conversationId);
        System.out.println("AI: " + response2);

        String newConversationId = "test_user_002_" + System.currentTimeMillis();
        String response3 = chatService.chat("What is my name?", newConversationId);
        System.out.println("New conversation AI: " + response3);

        assertThat(response1).isNotBlank();
        assertThat(response2).containsIgnoringCase("zhang").contains("Java");
        assertThat(response3).isNotBlank();
    }
}
