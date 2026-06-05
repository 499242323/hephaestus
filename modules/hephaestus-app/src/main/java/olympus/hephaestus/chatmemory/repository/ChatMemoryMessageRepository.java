package olympus.hephaestus.chatmemory.repository;

import olympus.hephaestus.chatmemory.entity.ChatMemoryMessageEntity;
import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import olympus.hephaestus.mybatis.repository.BaseInsertTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;

@Mapper
public interface ChatMemoryMessageRepository extends BaseAbstractRepository<ChatMemoryMessageEntity, Long> {

    @Select("""
            SELECT DISTINCT conversation_id
            FROM spring_ai_chat_memory
            ORDER BY conversation_id
            """)
    List<String> findConversationIds();

    @Select("""
            SELECT id,
                   conversation_id AS conversationId,
                   content,
                   type,
                   timestamp
            FROM spring_ai_chat_memory
            WHERE conversation_id = #{conversationId}
            ORDER BY timestamp, id
            """)
    List<ChatMemoryMessageEntity> findByConversationId(@Param("conversationId") String conversationId);

    @UpdateProvider(type = BaseInsertTemplate.class, method = "dynamicSQL")
    void insertList(@Param("_list") List<ChatMemoryMessageEntity> messages);

    @Delete("""
            DELETE FROM spring_ai_chat_memory
            WHERE conversation_id = #{conversationId}
            """)
    void deleteByConversationId(@Param("conversationId") String conversationId);
}
