package olympus.hephaestus.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiService {

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;

    public AiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                .call()
                .content();
    }

    public Flux<String> streamChat(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                .stream()
                .content();
    }
}
