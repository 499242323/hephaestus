package olympus.hephaestus.chatmemory.service;

import olympus.hephaestus.chatmemory.entity.ChatMemoryMessageEntity;
import olympus.hephaestus.chatmemory.repository.ChatMemoryMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MybatisChatMemoryRepositoryTest {

    @Test
    void savesAndReadsChatMessagesThroughRepositoryContract() {
        ChatMemoryMessageRepository repository = mock(ChatMemoryMessageRepository.class);
        MybatisChatMemoryRepository chatMemoryRepository = new MybatisChatMemoryRepository(repository);

        List<Message> messages = List.of(
                new UserMessage("hello"),
                new AssistantMessage("world"),
                new SystemMessage("system")
        );

        chatMemoryRepository.saveAll("c1", messages);

        verify(repository).insertList(org.mockito.ArgumentMatchers.argThat(items -> {
            List<ChatMemoryMessageEntity> entityList = (List<ChatMemoryMessageEntity>) items;
            return entityList.size() == 3
                    && "c1".equals(entityList.get(0).getConversationId())
                    && "hello".equals(entityList.get(0).getContent())
                    && "user".equals(entityList.get(0).getType());
        }));

        ChatMemoryMessageEntity first = new ChatMemoryMessageEntity();
        first.setId(1L);
        first.setConversationId("c1");
        first.setContent("hello");
        first.setType("USER");
        first.setTimestamp(LocalDateTime.now());

        ChatMemoryMessageEntity second = new ChatMemoryMessageEntity();
        second.setId(2L);
        second.setConversationId("c1");
        second.setContent("world");
        second.setType("assistant");
        second.setTimestamp(LocalDateTime.now());

        when(repository.findByConversationId("c1")).thenReturn(List.of(first, second));

        List<Message> restored = chatMemoryRepository.findByConversationId("c1");

        assertThat(restored).hasSize(2);
        assertThat(restored.get(0)).isInstanceOf(UserMessage.class);
        assertThat(restored.get(1)).isInstanceOf(AssistantMessage.class);
    }
}
