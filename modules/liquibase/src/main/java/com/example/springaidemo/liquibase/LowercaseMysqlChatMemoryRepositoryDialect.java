//package com.example.springaidemo.liquibase;
//
//import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
//
//public class LowercaseMysqlChatMemoryRepositoryDialect implements JdbcChatMemoryRepositoryDialect {
//
//    public static final String TABLE_NAME = "spring_ai_chat_memory";
//
//    @Override
//    public String getSelectMessagesSql() {
//        return "SELECT conversation_id, content, type, timestamp FROM " + TABLE_NAME
//                + " WHERE conversation_id = ? ORDER BY timestamp";
//    }
//
//    @Override
//    public String getInsertMessageSql() {
//        return "INSERT INTO " + TABLE_NAME
//                + " (conversation_id, content, type, timestamp) VALUES (?, ?, ?, ?)";
//    }
//
//    @Override
//    public String getSelectConversationIdsSql() {
//        return "SELECT DISTINCT conversation_id FROM " + TABLE_NAME + " ORDER BY conversation_id";
//    }
//
//    @Override
//    public String getDeleteMessagesSql() {
//        return "DELETE FROM " + TABLE_NAME + " WHERE conversation_id = ?";
//    }
//}
