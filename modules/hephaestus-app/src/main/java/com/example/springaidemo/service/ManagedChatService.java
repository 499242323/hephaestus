package com.example.springaidemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ManagedChatService {
    
    private final ChatClient chatClient;
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();
    
    public ManagedChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    /**
     * 带会话过期管理的对话
     */
    public String chatWithSession(String message, String userId) {
        String conversationId = getOrCreateConversation(userId);
        
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor
                    .param("chat_memory_conversation_id", conversationId)
                    .param("chat_memory_retrieve_size", 15)  // 仅检索最近15条消息
                )
                .call()
                .content();
    }
    
    private String getOrCreateConversation(String userId) {
        ConversationSession session = sessions.get(userId);
        
        if (session == null || session.isExpired()) {
            // 创建新会话（使用userId作为conversationId前缀确保隔离）
            String newConversationId = userId + "_" + System.currentTimeMillis();
            sessions.put(userId, new ConversationSession(newConversationId));
            return newConversationId;
        }
        
        session.refresh();
        return session.getConversationId();
    }
    
    private static class ConversationSession {
        private final String conversationId;
        private long lastAccessTime;
        private static final long TIMEOUT_MS = 30 * 60 * 1000; // 30分钟过期
        
        public ConversationSession(String conversationId) {
            this.conversationId = conversationId;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > TIMEOUT_MS;
        }
        
        public void refresh() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public String getConversationId() { return conversationId; }
    }
}
