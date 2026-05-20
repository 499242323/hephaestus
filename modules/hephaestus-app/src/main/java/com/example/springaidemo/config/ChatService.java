package com.example.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ChatService {

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 带记忆的多轮对话。
     *
     * @param message 用户消息
     * @param conversationId 会话 ID，用于区分不同用户或会话
     * @return AI 回复内容
     */
    public String chat(String message, String conversationId) {
        List<String> chunks = chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                .stream()
                .content()
                .collectList()
                .block(Duration.ofSeconds(60));

        return chunks == null ? "" : String.join("", chunks);
    }
}
