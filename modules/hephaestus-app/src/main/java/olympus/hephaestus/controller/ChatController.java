package olympus.hephaestus.controller;

import olympus.hephaestus.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final ChatService chatService;
    // 模拟会话管理（实际应用中应从Token/Session获取用户ID）
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping("/send")
    public ChatResponse sendMessage(@RequestBody ChatRequest request, 
                                     @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        // 实际场景：从认证信息获取用户ID
        String conversationId = sessionId != null ? sessionId : "default";
        
        String reply = chatService.chat(request.getMessage(), conversationId);
        return new ChatResponse(reply);
    }
    
    @PostMapping("/new-session")
    public NewSessionResponse newSession(@RequestHeader("X-User-Id") String userId) {
        // 生成新的会话ID
        String conversationId = userId + "_" + System.currentTimeMillis();
        return new NewSessionResponse(conversationId);
    }
    
    // DTO类
    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class ChatResponse {
        private String reply;
        public ChatResponse(String reply) { this.reply = reply; }
        public String getReply() { return reply; }
    }
    
    public static class NewSessionResponse {
        private String conversationId;
        public NewSessionResponse(String conversationId) { this.conversationId = conversationId; }
        public String getConversationId() { return conversationId; }
    }
}
