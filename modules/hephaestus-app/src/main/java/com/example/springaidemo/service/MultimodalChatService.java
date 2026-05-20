package com.example.springaidemo.service;

import com.example.springaidemo.media.MediaFile;
import com.example.springaidemo.media.MediaFileRepository;
import com.example.springaidemo.media.MediaStorageProperties;
import com.example.springaidemo.media.MediaStorageException;
import com.example.springaidemo.media.MediaStorageService;
import com.example.springaidemo.media.StoredMediaFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class MultimodalChatService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    static final String RESPONSE_SYSTEM_PROMPT = """
            你是 Hephaestus 的多模态聊天助手。
            请直接用中文回答用户问题。
            如果用户上传了附件，请结合附件内容回答。
            不要输出 JSON，不要输出 Markdown 包裹的结构化协议。
            """;
    static final String DECISION_SYSTEM_PROMPT = """
            你是 Hephaestus 的多模态聊天助手。请根据用户输入和可选附件内容返回严格 JSON，不要输出 Markdown。

            返回格式必须是：
            {
              "generateImage": false,
              "reply": "中文回复内容",
              "imagePrompt": "当需要生成图片时，给图片模型使用的中文或英文 prompt；不需要生成图片时为空字符串"
            }

            判断规则：
            1. 如果用户明确要求生成、绘制、画图、做海报、做插画、根据附件生成图片，则 generateImage=true。
            2. 如果用户只是提问、总结、分析、解释附件，则 generateImage=false。
            3. 如果 generateImage=true，请把用户要求和附件关键信息整理成清晰的 imagePrompt。
            4. 如果模型无法理解附件，请在 reply 中用中文说明。
            """;

    private final ChatClient chatClient;
    private final ObjectProvider<ImageModel> imageModelProvider;
    private final MediaStorageService mediaStorageService;
    private final MediaFileRepository mediaFileRepository;
    private final MediaStorageProperties mediaStorageProperties;

    public MultimodalChatService(ChatClient chatClient,
                                 ObjectProvider<ImageModel> imageModelProvider,
                                 MediaStorageService mediaStorageService,
                                 MediaFileRepository mediaFileRepository,
                                 MediaStorageProperties mediaStorageProperties) {
        this.chatClient = chatClient;
        this.imageModelProvider = imageModelProvider;
        this.mediaStorageService = mediaStorageService;
        this.mediaFileRepository = mediaFileRepository;
        this.mediaStorageProperties = mediaStorageProperties;
    }

    public MultimodalResponse chat(String message, MultipartFile file, String conversationId) {
        String normalizedMessage = message == null ? "" : message.trim();
        List<MediaResource> attachments = new ArrayList<>();
        if (file != null && !file.isEmpty()) {
            attachments.add(storeUploadedFile(file, conversationId));
        }

        String prompt = buildDecisionUserMessage(normalizedMessage, file);
        String rawDecision = callDecisionModel(prompt, file, conversationId);
        Decision decision = parseDecision(rawDecision);

        if (!decision.generateImage()) {
            return new MultimodalResponse("TEXT", decision.reply(), false, "", null, attachments, null);
        }

        String imagePrompt = decision.imagePrompt().isBlank()
                ? fallbackImagePrompt(normalizedMessage, file)
                : decision.imagePrompt();
        try {
            MediaResource generatedImage = generateAndStoreImage(imagePrompt, conversationId);
            return new MultimodalResponse("IMAGE", decision.reply(), true, imagePrompt, generatedImage.url(), attachments, generatedImage);
        } catch (Exception exception) {
            String reply = (decision.reply().isBlank() ? "已完成文字处理。" : decision.reply())
                    + " 但图片生成失败：" + exception.getMessage();
            return new MultimodalResponse("TEXT", reply, false, imagePrompt, null, attachments, null);
        }
    }

    public StreamPlan prepareStream(String message, MultipartFile file, String conversationId) {
        String normalizedMessage = message == null ? "" : message.trim();
        List<MediaResource> attachments = new ArrayList<>();
        if (file != null && !file.isEmpty()) {
            attachments.add(storeUploadedFile(file, conversationId));
        }

        if (shouldStreamTextDirectly(normalizedMessage)) {
            return StreamPlan.text(attachments, streamTextReply(normalizedMessage, file, conversationId));
        }

        String prompt = buildDecisionUserMessage(normalizedMessage, file);
        String rawDecision = callDecisionModel(prompt, file, conversationId);
        Decision decision = parseDecision(rawDecision);

        if (!decision.generateImage()) {
            return StreamPlan.text(attachments, streamTextReply(normalizedMessage, file, conversationId));
        }

        String imagePrompt = decision.imagePrompt().isBlank()
                ? fallbackImagePrompt(normalizedMessage, file)
                : decision.imagePrompt();
        try {
            MediaResource generatedImage = generateAndStoreImage(imagePrompt, conversationId);
            Flux<String> replyFlux = decision.reply().isBlank() ? Flux.empty() : Flux.just(decision.reply());
            return StreamPlan.image(attachments, replyFlux, imagePrompt, generatedImage);
        } catch (Exception exception) {
            String reply = (decision.reply().isBlank() ? "已完成文字处理。" : decision.reply())
                    + " 但图片生成失败：" + exception.getMessage();
            return StreamPlan.text(attachments, Flux.just(reply));
        }
    }

    private boolean shouldStreamTextDirectly(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase();
        if (normalized.isBlank()) {
            return true;
        }
        return !containsImageGenerationIntent(normalized);
    }

    private boolean containsImageGenerationIntent(String message) {
        return message.contains("生成图片")
                || message.contains("生成一张")
                || message.contains("画一张")
                || message.contains("画个")
                || message.contains("绘制")
                || message.contains("海报")
                || message.contains("插画")
                || message.contains("封面图")
                || message.contains("配图")
                || message.contains("image")
                || message.contains("poster")
                || message.contains("illustration");
    }

    public static Decision parseDecision(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        String json = extractJson(normalized);
        if (json.isBlank()) {
            return Decision.text(normalized);
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            boolean generateImage = node.path("generateImage").asBoolean(false);
            String reply = node.path("reply").asText("");
            String imagePrompt = node.path("imagePrompt").asText("");
            return new Decision(generateImage, reply, imagePrompt);
        } catch (Exception ignored) {
            return Decision.text(normalized);
        }
    }

    static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "";
    }

    private String callDecisionModel(String prompt, MultipartFile file, String conversationId) {
        if (file == null || file.isEmpty()) {
            return collectContent(chatClient.prompt()
                    .system(DECISION_SYSTEM_PROMPT)
                    .user(prompt)
                    .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content()
                    .collectList()
                    .block(Duration.ofSeconds(60)));
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType(file));
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            return collectContent(chatClient.prompt()
                    .system(DECISION_SYSTEM_PROMPT)
                    .user(user -> user
                            .text(prompt)
                            .media(mediaType, resource))
                    .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content()
                    .collectList()
                    .block(Duration.ofSeconds(60)));
        } catch (Exception exception) {
            log.error("模型暂时无法解析该附件: 会话ID={}, 附件名={}", conversationId, file.getOriginalFilename(), exception);
            return "模型暂时无法解析该附件：";
        }
    }

    private Flux<String> streamTextReply(String message, MultipartFile file, String conversationId) {
        String normalizedMessage = message == null || message.isBlank() ? "请结合附件内容进行分析并直接回答。" : message;
        if (file == null || file.isEmpty()) {
            return chatClient.prompt()
                    .system(RESPONSE_SYSTEM_PROMPT)
                    .user(normalizedMessage)
                    .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content();
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType(file));
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            return chatClient.prompt()
                    .system(RESPONSE_SYSTEM_PROMPT)
                    .user(user -> user
                            .text(normalizedMessage)
                            .media(mediaType, resource))
                    .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content();
        } catch (Exception exception) {
            log.error("文本流式回复准备失败: 会话ID={}, 附件名={}", conversationId, file.getOriginalFilename(), exception);
            return Flux.just("暂时无法读取附件内容，请稍后重试。");
        }
    }

    private String generateImage(String imagePrompt) {
        ImageModel imageModel = imageModelProvider.getIfAvailable();
        if (imageModel == null) {
            return buildFallbackImageDataUrl(imagePrompt);
        }

        try {
            ImageResponse response = imageModel.call(new ImagePrompt(imagePrompt));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return buildFallbackImageDataUrl(imagePrompt);
            }

            org.springframework.ai.image.Image image = response.getResult().getOutput();
            if (image.getUrl() != null && !image.getUrl().isBlank()) {
                return image.getUrl();
            }
            if (image.getB64Json() != null && !image.getB64Json().isBlank()) {
                return "data:image/png;base64," + image.getB64Json();
            }
        } catch (Exception ignored) {
            return buildFallbackImageDataUrl(imagePrompt);
        }
        return buildFallbackImageDataUrl(imagePrompt);
    }

    private MediaResource generateAndStoreImage(String imagePrompt, String conversationId) {
        String imageUrl = generateImage(imagePrompt);
        try {
            StoredMediaFile storedFile = imageUrl.startsWith("data:")
                    ? mediaStorageService.uploadImageFromDataUrl(conversationId, imageUrl)
                    : mediaStorageService.uploadImageFromUrl(conversationId, imageUrl);
            return saveMediaFile(conversationId, storedFile, "AI_GENERATED_IMAGE");
        } catch (Exception exception) {
            log.error("生成图片后保存失败，回退为外链: 会话ID={}, 图片地址={}", conversationId, imageUrl, exception);
            return externalImageResource(imageUrl);
        }
    }

    private MediaResource externalImageResource(String imageUrl) {
        return new MediaResource(
                null,
                "generated-image.png",
                MediaType.IMAGE_PNG_VALUE,
                0,
                imageUrl,
                imageUrl
        );
    }

    private String buildFallbackImageDataUrl(String prompt) {
        return "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(buildFallbackSvg(prompt).getBytes(StandardCharsets.UTF_8));
    }

    private String buildFallbackSvg(String prompt) {
        int hash = Math.abs(prompt.hashCode());
        String colorA = String.format("#%06x", 0x335577 + hash % 0x555555);
        String colorB = String.format("#%06x", 0x668855 + (hash / 7) % 0x444444);
        String safePrompt = escapeXml(prompt.length() > 48 ? prompt.substring(0, 48) + "..." : prompt);

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="960" height="540" viewBox="0 0 960 540">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="960" height="540" fill="url(#bg)"/>
                  <circle cx="760" cy="120" r="72" fill="rgba(255,255,255,0.22)"/>
                  <rect x="96" y="110" width="768" height="320" rx="28" fill="rgba(255,255,255,0.82)"/>
                  <text x="140" y="210" fill="#1f2937" font-family="Microsoft YaHei, Arial" font-size="38" font-weight="700">Spring AI Image</text>
                  <text x="140" y="280" fill="#374151" font-family="Microsoft YaHei, Arial" font-size="28">%s</text>
                  <text x="140" y="348" fill="#6b7280" font-family="Microsoft YaHei, Arial" font-size="22">Demo generated image</text>
                </svg>
                """.formatted(colorA, colorB, safePrompt);
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private MediaResource storeUploadedFile(MultipartFile file, String conversationId) {
        try {
            StoredMediaFile storedFile = mediaStorageService.upload(conversationId, file);
            return saveMediaFile(conversationId, storedFile, "USER_UPLOAD");
        }  catch (Exception exception) {
            log.error("用户附件保存流程出现未预期异常: 会话ID={}, 附件名={}",
                    conversationId,
                    file.getOriginalFilename(),
                    exception);
            throw new MediaStorageException("附件保存到多媒体服务失败", exception);
        }
    }

    private MediaResource saveMediaFile(String conversationId, StoredMediaFile storedFile, String sourceType) {
        MediaFile saved = mediaFileRepository.save(new MediaFile(
                null,
                conversationId,
                storedFile.originalFilename(),
                storedFile.storedFilename(),
                storedFile.contentType(),
                storedFile.fileSize(),
                storedFile.storagePath(),
                "",
                sourceType,
                LocalDateTime.now()
        ));
        String url = buildAccessUrl(storedFile.storagePath());
        mediaFileRepository.updateAccessUrl(saved.id(), url);
        log.info("附件元数据写入成功: 会话ID={}, 媒体ID={}, 来源类型={}, 访问地址={}",
                conversationId,
                saved.id(),
                sourceType,
                url);
        return new MediaResource(
                saved.id(),
                saved.originalFilename(),
                saved.contentType(),
                saved.fileSize(),
                url,
                url
        );
    }

    private String buildAccessUrl(String storagePath) {
        String prefix = mediaStorageProperties.getAccessPathPrefix();
        String normalizedPrefix = prefix == null || prefix.isBlank() ? "/mediadl/media/getdata" : prefix.trim();
        while (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        String normalizedPath = storagePath == null ? "" : storagePath.trim();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedPrefix + "/" + normalizedPath;
    }

    private static String collectContent(List<String> chunks) {
        return chunks == null ? "" : String.join("", chunks);
    }

    private static String fileSummary(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "未上传附件";
        }
        return """
                文件名：%s
                MIME 类型：%s
                文件大小：%d bytes
                """.formatted(
                file.getOriginalFilename() == null ? "未命名附件" : file.getOriginalFilename(),
                contentType(file),
                file.getSize()
        );
    }

    private static String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getContentType();
    }

    private static String fallbackImagePrompt(String message, MultipartFile file) {
        String base = message == null || message.isBlank() ? "根据上传附件生成一张图片" : message;
        return base + "\n\n附件信息：\n" + fileSummary(file);
    }

    static String buildDecisionUserMessage(String message, MultipartFile file) {
        return """
                文件信息：
                %s

                用户输入：
                %s
                """.formatted(fileSummary(file), message == null ? "" : message);
    }

    public record Decision(boolean generateImage, String reply, String imagePrompt) {
        static Decision text(String reply) {
            return new Decision(false, reply == null ? "" : reply, "");
        }
    }

    public record MultimodalResponse(
            String type,
            String reply,
            boolean generateImage,
            String imagePrompt,
            String imageUrl,
            List<MediaResource> attachments,
            MediaResource generatedImage
    ) {
    }

    public record StreamPlan(
            String type,
            List<MediaResource> attachments,
            Flux<String> replyFlux,
            boolean generateImage,
            String imagePrompt,
            MediaResource generatedImage
    ) {
        static StreamPlan text(List<MediaResource> attachments, Flux<String> replyFlux) {
            return new StreamPlan("TEXT", attachments, replyFlux, false, "", null);
        }

        static StreamPlan image(List<MediaResource> attachments, Flux<String> replyFlux, String imagePrompt, MediaResource generatedImage) {
            return new StreamPlan("IMAGE", attachments, replyFlux, true, imagePrompt, generatedImage);
        }
    }

    public record MediaResource(
            Long id,
            String fileName,
            String contentType,
            long fileSize,
            String url,
            String downloadUrl
    ) {
    }
}
