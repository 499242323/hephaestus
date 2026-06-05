package olympus.hephaestus.chatmemory.service;

import olympus.hephaestus.chatmemory.entity.ChatMemoryMessageEntity;
import olympus.hephaestus.chatmemory.repository.ChatMemoryMessageRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MybatisChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryMessageRepository repository;

    public MybatisChatMemoryRepository(ChatMemoryMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> findConversationIds() {
        return repository.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return repository.findByConversationId(conversationId).stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<ChatMemoryMessageEntity> entities = new ArrayList<>(messages.size());
        LocalDateTime now = LocalDateTime.now();
        for (Message message : messages) {
            ChatMemoryMessageEntity entity = new ChatMemoryMessageEntity();
            entity.setConversationId(conversationId);
            entity.setContent(extractText(message));
            entity.setType(message.getMessageType().getValue());
            entity.setTimestamp(now);
            entities.add(entity);
        }
        repository.insertList(entities);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }

    private Message toMessage(ChatMemoryMessageEntity entity) {
        MessageType messageType = resolveMessageType(entity.getType());
        return switch (messageType) {
            case USER -> new UserMessage(entity.getContent());
            case ASSISTANT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
            case TOOL -> new AssistantMessage(entity.getContent());
        };
    }

    private MessageType resolveMessageType(String type) {
        if (type == null || type.isBlank()) {
            return MessageType.USER;
        }
        try {
            return MessageType.fromValue(type);
        } catch (IllegalArgumentException ignored) {
            return MessageType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        }
    }

    private String extractText(Message message) {
        if (message instanceof org.springframework.ai.chat.messages.AbstractMessage abstractMessage) {
            return abstractMessage.getText();
        }
        return "";
    }
}
