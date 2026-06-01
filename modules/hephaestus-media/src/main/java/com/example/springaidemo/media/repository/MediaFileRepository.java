package com.example.springaidemo.media.repository;

import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.entity.MediaFileEntity;
import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import com.example.springaidemo.mybatis.repository.BaseInsertTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MediaFileRepository extends BaseAbstractRepository<MediaFileEntity, Long> {

    @Select("""
            SELECT id,
                   conversation_id AS conversationId,
                   original_filename AS originalFilename,
                   stored_filename AS storedFilename,
                   content_type AS contentType,
                   file_size AS fileSize,
                   storage_path AS storagePath,
                   access_url AS accessUrl,
                   source_type AS sourceType,
                   created_at AS createdAt
            FROM spring_ai_media_file
            WHERE id = #{id}
            """)
    MediaFileEntity getById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id,
                   conversation_id AS conversationId,
                   original_filename AS originalFilename,
                   stored_filename AS storedFilename,
                   content_type AS contentType,
                   file_size AS fileSize,
                   storage_path AS storagePath,
                   access_url AS accessUrl,
                   source_type AS sourceType,
                   created_at AS createdAt
            FROM spring_ai_media_file
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<MediaFileEntity> findEntitiesByIds(@Param("ids") List<Long> ids);

    @Update("UPDATE spring_ai_media_file SET access_url = #{accessUrl} WHERE id = #{id}")
    void updateAccessUrl(@Param("id") long id, @Param("accessUrl") String accessUrl);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @UpdateProvider(type = BaseInsertTemplate.class, method = "dynamicSQL")
    void insertList(@Param("_list") List<MediaFileEntity> mediaFileList);

    default MediaFile saveMediaFile(MediaFile mediaFile) {
        MediaFileEntity entity = MediaFileEntity.fromDomain(mediaFile);
        save(entity);
        return entity.toDomain();
    }

    default MediaFile updateMediaFile(MediaFile mediaFile) {
        MediaFileEntity entity = MediaFileEntity.fromDomain(mediaFile);
        update(entity);
        return entity.toDomain();
    }

    default Optional<MediaFile> findById(long id) {
        MediaFileEntity entity = getById(id);
        return entity == null ? Optional.empty() : Optional.of(entity.toDomain());
    }

    default List<MediaFile> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return findEntitiesByIds(ids).stream().map(MediaFileEntity::toDomain).toList();
    }

    default void deleteMediaFile(long id) {
        removeById(id);
    }
}
