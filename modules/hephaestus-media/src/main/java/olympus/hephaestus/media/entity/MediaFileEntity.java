package olympus.hephaestus.media.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import olympus.hephaestus.media.domain.MediaFile;

import java.time.LocalDateTime;

@TableName("spring_ai_media_file")
public class MediaFileEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("stored_filename")
    private String storedFilename;

    @TableField("content_type")
    private String contentType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("storage_path")
    private String storagePath;

    @TableField("access_url")
    private String accessUrl;

    @TableField("source_type")
    private String sourceType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public static MediaFileEntity fromDomain(MediaFile mediaFile) {
        MediaFileEntity entity = new MediaFileEntity();
        entity.setId(mediaFile.id());
        entity.setConversationId(mediaFile.conversationId());
        entity.setOriginalFilename(mediaFile.originalFilename());
        entity.setStoredFilename(mediaFile.storedFilename());
        entity.setContentType(mediaFile.contentType());
        entity.setFileSize(mediaFile.fileSize());
        entity.setStoragePath(mediaFile.storagePath());
        entity.setAccessUrl(mediaFile.accessUrl());
        entity.setSourceType(mediaFile.sourceType());
        entity.setCreatedAt(mediaFile.createdAt());
        return entity;
    }

    public MediaFile toDomain() {
        return new MediaFile(
                id,
                conversationId,
                originalFilename,
                storedFilename,
                contentType,
                fileSize == null ? 0L : fileSize,
                storagePath,
                accessUrl,
                sourceType,
                createdAt
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
