package com.example.springaidemo.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.springaidemo.media.config.MediaStorageProperties;
import com.example.springaidemo.media.domain.MediaFile;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.exception.MediaStorageException;
import com.example.springaidemo.media.service.MediaFileService;
import com.example.springaidemo.media.service.MediaStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class MultimodalChatService {
    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    private static final int MAX_INLINE_TEXT_ATTACHMENT_BYTES = 256 * 1024;
    private static final int MAX_DECISION_ATTACHMENT_PREVIEW_CHARS = 4000;
    private static final Set<String> TEXT_CONTENT_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "text/html",
            "text/xml",
            "application/json",
            "application/xml",
            "application/javascript",
            "application/x-javascript",
            "application/typescript",
            "application/x-yaml",
            "application/yaml",
            "application/toml"
    );
    private static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "json", "xml", "yaml", "yml", "html", "htm",
            "csv", "log", "sql", "java", "js", "ts", "css", "properties"
    );

    static final String RESPONSE_SYSTEM_PROMPT = """
            你是 Hephaestus 的多模态聊天助手。
            请始终使用中文回复用户。
            如果用户上传了附件，请结合附件内容进行回答。
            如果用户询问今天的天气、实时天气、气温、是否下雨、空气情况等实时天气信息，优先调用天气、位置工具获取真实数据。
            回答内容完整格式不要JSON,回复不含生图策略，直接回答文字内容。
            如果需要返回 Markdown、HTML 片段、代码块或 JSON，请直接输出原始内容，不要额外包一层 JSON 协议。
            """;

    static final String IMAGE_REPLY_SYSTEM_PROMPT = """
            你是 Hephaestus 的多模态聊天助手。
            请始终使用中文回复用户。
            系统会负责生成图片，你只需要输出给用户看的最终中文说明或文案。
            不要输出 image_prompt、prompt、JSON、字段名、调试信息或内部推理过程。
            如果用户已经给了完整画面描述，就直接输出简短自然的确认或补充说明即可。
            """;

    static final String DECISION_SYSTEM_PROMPT = """
            你是 Hephaestus 的多模态聊天助手。请根据当前用户输入、历史对话上下文和可选附件内容返回严格 JSON。
            reply 字段必须使用中文，不要回复是否生成图片策略。
            返回格式必须是：
            {
              "generateImage": false,
              "reply": "中文回复内容",
              "imagePrompt": "当需要生成图片时，给图片模型使用的中文或英文 prompt；不需要生成图片时为空字符串"
            }

            判断规则：
            1. 如果用户明确要求生成、绘制、画图、做海报、做插画、根据附件生成图片，则 generateImage=true。
            2. 如果用户提供了一整段画面描述、角色设定、场景设定、风格要求，即使没有明确写“生成图片”，通常也应判断为 generateImage=true。
            3. 如果上一轮助手正在帮助用户细化图片方案，并给出了 1/2/3/4 之类的风格、版本、类型选项，那么当前用户只回复“1”“2”“3”“4”“第一种”“第二种”“选2”这类短选择时，也要结合历史上下文判断为 generateImage=true，而不是继续追问。
            4. 只有当用户明确表示不要图片、只要文字、只做总结、分析、解释时，才判断为 generateImage=false。
            5. 如果 generateImage=true，请把用户要求、历史上下文和附件关键信息整理成清晰完整的 imagePrompt。
            6. 如果模型无法理解附件，请在 reply 中用中文说明。
            """;

    private final ChatClient chatClient;
    private final ObjectProvider<ImageModel> imageModelProvider;
    private final MediaStorageService mediaStorageService;
    private final MediaFileService mediaFileService;
    private final MediaStorageProperties mediaStorageProperties;

    public MultimodalChatService(ChatClient chatClient,
                                 ObjectProvider<ImageModel> imageModelProvider,
                                 MediaStorageService mediaStorageService,
                                 MediaFileService mediaFileService,
                                 MediaStorageProperties mediaStorageProperties) {
        this.chatClient = chatClient;
        this.imageModelProvider = imageModelProvider;
        this.mediaStorageService = mediaStorageService;
        this.mediaFileService = mediaFileService;
        this.mediaStorageProperties = mediaStorageProperties;
    }

    public MultimodalResponse chat(String message, MultipartFile file, String conversationId) {
        ChatRequestContext context = buildChatRequestContext(message, file, conversationId);
        if (shouldDirectGenerateImage(context.message())) {
            return imageResponse(context, file, conversationId, null);
        }

        if (shouldConsultDecisionModel(context)) {
            Decision decision = decide(context, file, conversationId);
            if (decision.generateImage()) {
                return imageResponse(context, file, conversationId, decision.imagePrompt());
            }
        }

        String reply = collectContent(streamTextReply(context.message(), file, context.inlineAttachmentText(), conversationId)
                .collectList()
                .block(Duration.ofSeconds(60)));
        return new MultimodalResponse("TEXT", reply, false, "", null, context.attachments(), null);
    }

    public StreamPlan prepareStream(String message, MultipartFile file, String conversationId) {
        ChatRequestContext context = buildChatRequestContext(message, file, conversationId);
        if (shouldDirectGenerateImage(context.message())) {
            return imageStreamPlan(context, file, conversationId, null);
        }

        if (shouldConsultDecisionModel(context)) {
            Decision decision = decide(context, file, conversationId);
            if (decision.generateImage()) {
                return imageStreamPlan(context, file, conversationId, decision.imagePrompt());
            }
        }

        return StreamPlan.text(
                context.attachments(),
                streamTextReply(context.message(), file, context.inlineAttachmentText(), conversationId)
        );
    }

    private MultimodalResponse imageResponse(ChatRequestContext context,
                                             MultipartFile file,
                                             String conversationId,
                                             String preferredImagePrompt) {
        String imagePrompt = resolveImagePrompt(context, file, preferredImagePrompt);
        String reply = collectContent(streamImageCompanionReply(context.message(), file, context.inlineAttachmentText(), conversationId)
                .collectList()
                .block(Duration.ofSeconds(60)));
        try {
            MediaResource generatedImage = generateAndStoreImage(imagePrompt, conversationId);
            return new MultimodalResponse("IMAGE", reply, true, imagePrompt, generatedImage.url(), context.attachments(), generatedImage);
        } catch (Exception exception) {
            String fallbackReply = (reply == null || reply.isBlank() ? "已完成文字处理。" : reply)
                    + " 但图片生成失败：" + exception.getMessage();
            return new MultimodalResponse("TEXT", fallbackReply, false, imagePrompt, null, context.attachments(), null);
        }
    }

    private StreamPlan imageStreamPlan(ChatRequestContext context,
                                       MultipartFile file,
                                       String conversationId,
                                       String preferredImagePrompt) {
        String imagePrompt = resolveImagePrompt(context, file, preferredImagePrompt);
        Flux<String> replyFlux = streamImageCompanionReply(context.message(), file, context.inlineAttachmentText(), conversationId);
        Mono<MediaResource> generatedImageMono = Mono.fromCallable(() -> generateAndStoreImage(imagePrompt, conversationId))
                .subscribeOn(Schedulers.boundedElastic());
        return StreamPlan.image(context.attachments(), replyFlux, imagePrompt, generatedImageMono);
    }

    private ChatRequestContext buildChatRequestContext(String message, MultipartFile file, String conversationId) {
        String normalizedMessage = message == null ? "" : message.trim();
        List<MediaResource> attachments = new ArrayList<>();
        String inlineAttachmentText = "";

        if (file != null && !file.isEmpty()) {
            attachments.add(storeUploadedFile(file, conversationId));
            inlineAttachmentText = readTextAttachment(file);
        }

        return new ChatRequestContext(normalizedMessage, attachments, inlineAttachmentText);
    }

    private boolean shouldDirectGenerateImage(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return containsImageGenerationIntent(normalized);
    }

    private boolean shouldConsultDecisionModel(ChatRequestContext context) {
        if (context == null) {
            return false;
        }
        if (context.hasAttachment()) {
            return true;
        }
        String normalized = context.message() == null ? "" : context.message().trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return !containsExplicitNoImageIntent(normalized);
    }

    private boolean containsImageGenerationIntent(String message) {
        return message.contains("生成图片")
                || message.contains("生成一张")
                || message.contains("生成一幅")
                || message.contains("画一张")
                || message.contains("画个")
                || message.contains("画一个")
                || message.contains("绘制")
                || message.contains("来一张")
                || message.contains("做张图")
                || message.contains("海报")
                || message.contains("插画")
                || message.contains("封面图")
                || message.contains("帮我生成")
                || message.contains("给我生成")
                || message.contains("帮我用这段话生成")
                || message.contains("参考这张图生成")
                || message.contains("参考附件生成")
                || message.contains("image")
                || message.contains("poster")
                || message.contains("illustration");
    }

    private boolean containsExplicitNoImageIntent(String message) {
        return message.contains("不要图片")
                || message.contains("不用图片")
                || message.contains("不需要图片")
                || message.contains("不要生成图片")
                || message.contains("不用生成图片")
                || message.contains("仅文字")
                || message.contains("只要文字")
                || message.contains("纯文字")
                || message.contains("不要配图")
                || message.contains("只分析")
                || message.contains("只总结")
                || message.contains("只解释");
    }

    private Decision decide(ChatRequestContext context, MultipartFile file, String conversationId) {
        String prompt = buildDecisionUserMessage(context);
        String rawDecision = callDecisionModel(prompt, file, context.inlineAttachmentText(), conversationId);
        return parseDecision(rawDecision);
    }

    private Flux<String> streamTextReply(String message, MultipartFile file, String inlineAttachmentText, String conversationId) {
        String normalizedMessage = message == null || message.isBlank() ? "请结合附件内容进行分析并直接回答。" : message;
        return streamReply(normalizedMessage, file, inlineAttachmentText, conversationId, RESPONSE_SYSTEM_PROMPT);
    }

    private Flux<String> streamImageCompanionReply(String message, MultipartFile file, String inlineAttachmentText, String conversationId) {
        String normalizedMessage = message == null || message.isBlank() ? "请围绕正在生成的图片输出最终展示给用户的中文内容。" : message;
        return streamReply(normalizedMessage, file, inlineAttachmentText, conversationId, IMAGE_REPLY_SYSTEM_PROMPT);
    }

    private Flux<String> streamReply(String message,
                                     MultipartFile file,
                                     String inlineAttachmentText,
                                     String conversationId,
                                     String systemPrompt) {
        String normalizedMessage = message == null || message.isBlank() ? "请直接回答。" : message;
        if (file == null || file.isEmpty() || !inlineAttachmentText.isBlank()) {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(buildPromptWithInlineAttachment(normalizedMessage, inlineAttachmentText))
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
                    .system(systemPrompt)
                    .user(user -> user.text(normalizedMessage).media(mediaType, resource))
                    .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content();
        } catch (Exception exception) {
            log.error("文本流式回复准备失败，conversationId={}, fileName={}", conversationId, file.getOriginalFilename(), exception);
            return Flux.just("暂时无法读取附件内容，请稍后重试。");
        }
    }

    private String readTextAttachment(MultipartFile file) {
        if (!isTextAttachment(file)) {
            return "";
        }

        if (file.getSize() > MAX_INLINE_TEXT_ATTACHMENT_BYTES) {
            return "[文本附件过大，已跳过直接内联读取，请基于已上传附件继续处理]";
        }

        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            return text.isBlank() ? "" : text;
        } catch (Exception exception) {
            log.error("读取文本附件失败，fileName={}", file.getOriginalFilename(), exception);
            return "";
        }
    }

    private boolean isTextAttachment(MultipartFile file) {
        String contentType = contentType(file).toLowerCase(Locale.ROOT);
        if (contentType.startsWith("text/") || TEXT_CONTENT_TYPES.contains(contentType)) {
            return true;
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            return false;
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return TEXT_FILE_EXTENSIONS.contains(extension);
    }

    private String buildPromptWithInlineAttachment(String prompt, String inlineAttachmentText) {
        if (inlineAttachmentText == null || inlineAttachmentText.isBlank()) {
            return prompt;
        }
        return prompt + "\n\n附件文本内容：\n```text\n" + inlineAttachmentText + "\n```";
    }

    private String callDecisionModel(String prompt, MultipartFile file, String inlineAttachmentText, String conversationId) {
        if (file == null || file.isEmpty() || !inlineAttachmentText.isBlank()) {
            return collectContent(chatClient.prompt()
                    .system(DECISION_SYSTEM_PROMPT)
                    .user(buildPromptWithInlineAttachment(prompt, inlineAttachmentText))
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
                    .user(user -> user.text(prompt).media(mediaType, resource))
                    .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content()
                    .collectList()
                    .block(Duration.ofSeconds(60)));
        } catch (Exception exception) {
            log.error("模型暂时无法解析该附件，conversationId={}, fileName={}", conversationId, file.getOriginalFilename(), exception);
            return "";
        }
    }

    static Decision parseDecision(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        String json = extractJson(normalized);
        if (json.isBlank()) {
            return Decision.text(normalized);
        }

        try {
            JSONObject node = JSON.parseObject(json);
            boolean generateImage = node.getBooleanValue("generateImage");
            String reply = node.getString("reply");
            String imagePrompt = node.getString("imagePrompt");
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

    private String resolveImagePrompt(ChatRequestContext context, MultipartFile file, String preferredImagePrompt) {
        if (preferredImagePrompt != null && !preferredImagePrompt.isBlank()) {
            return preferredImagePrompt;
        }
        return buildImagePrompt(context.message(), context.inlineAttachmentText(), file);
    }

    private String buildImagePrompt(String message, String inlineAttachmentText, MultipartFile file) {
        String base = message == null || message.isBlank() ? "根据上传附件生成一张图片" : message;
        if (inlineAttachmentText != null && !inlineAttachmentText.isBlank()) {
            return base + "\n\n附件文本内容：\n" + inlineAttachmentText;
        }
        return base + "\n\n附件信息：\n" + fileSummary(file);
    }

    static String buildDecisionUserMessage(ChatRequestContext context) {
        String attachmentSummary = context == null || !context.hasAttachment()
                ? "未上传附件"
                : context.attachmentSummary();
        String attachmentTextSnippet = context == null ? "" : context.decisionAttachmentPreview();
        return """
                请基于以下结构化信息和历史对话判断本轮是否需要生成图片。
                [userMessage]
                %s

                [hasAttachment]
                %s

                [attachmentSummary]
                %s

                [attachmentTextSnippet]
                %s
                """.formatted(
                context == null || context.message() == null ? "" : context.message(),
                context != null && context.hasAttachment(),
                attachmentSummary,
                attachmentTextSnippet.isBlank() ? "(empty)" : attachmentTextSnippet
        );
    }

    private String generateImage(String imagePrompt) {
        long modelStartAt = System.currentTimeMillis();
        ImageModel imageModel = imageModelProvider.getIfAvailable();
        if (imageModel == null) {
            log.info("图片模型未配置，使用兜底图片，模型耗时={}ms", System.currentTimeMillis() - modelStartAt);
            return buildFallbackImageDataUrl(imagePrompt);
        }

        try {
            ImageResponse response = imageModel.call(new ImagePrompt(imagePrompt));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("图片模型返回空结果，改用兜底图片，模型耗时={}ms", System.currentTimeMillis() - modelStartAt);
                return buildFallbackImageDataUrl(imagePrompt);
            }

            org.springframework.ai.image.Image image = response.getResult().getOutput();
            if (image.getUrl() != null && !image.getUrl().isBlank()) {
                log.info("图片模型生成完成，返回外链，模型耗时={}ms", System.currentTimeMillis() - modelStartAt);
                return image.getUrl();
            }
            if (image.getB64Json() != null && !image.getB64Json().isBlank()) {
                log.info("图片模型生成完成，返回Base64，模型耗时={}ms", System.currentTimeMillis() - modelStartAt);
                return "data:image/png;base64," + image.getB64Json();
            }
        } catch (Exception exception) {
            log.error("图片模型生成失败，改用兜底图片，模型耗时={}ms", System.currentTimeMillis() - modelStartAt, exception);
            return buildFallbackImageDataUrl(imagePrompt);
        }
        log.warn("图片模型未返回可用内容，改用兜底图片，模型耗时={}ms", System.currentTimeMillis() - modelStartAt);
        return buildFallbackImageDataUrl(imagePrompt);
    }

    private MediaResource generateAndStoreImage(String imagePrompt, String conversationId) {
        long totalStartAt = System.currentTimeMillis();
        String imageUrl = generateImage(imagePrompt);
        try {
            long uploadStartAt = System.currentTimeMillis();
            StoredMediaFile storedFile = imageUrl.startsWith("data:")
                    ? mediaStorageService.uploadImageFromDataUrl(conversationId, imageUrl)
                    : mediaStorageService.uploadImageFromUrl(conversationId, imageUrl);
            MediaResource resource = saveMediaFile(conversationId, storedFile, "AI_GENERATED_IMAGE");
            log.info("AI图片处理完成，conversationId={}, 上传耗时={}ms, 总耗时={}ms, 返回类型={}",
                    conversationId,
                    System.currentTimeMillis() - uploadStartAt,
                    System.currentTimeMillis() - totalStartAt,
                    imageUrl.startsWith("data:") ? "base64" : "url");
            return resource;
        } catch (Exception exception) {
            log.error("AI图片生成后保存失败，conversationId={}, 总耗时={}ms, imageUrl={}",
                    conversationId,
                    System.currentTimeMillis() - totalStartAt,
                    imageUrl,
                    exception);
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
        } catch (Exception exception) {
            log.error("用户附件保存流程出现异常，conversationId={}, fileName={}", conversationId, file.getOriginalFilename(), exception);
            throw new MediaStorageException("附件保存到多媒体服务失败", exception);
        }
    }

    private MediaResource saveMediaFile(String conversationId, StoredMediaFile storedFile, String sourceType) {
        String url = buildAccessUrl(storedFile.storagePath());
        MediaFile saved = mediaFileService.save(conversationId, storedFile, sourceType, url);
        log.info("附件元数据写入成功，conversationId={}, mediaId={}, sourceType={}, accessUrl={}",
                conversationId, saved.id(), sourceType, url);
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

    private static String contentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getContentType();
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

    private static String safeDecisionReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return "已完成分析。";
        }
        return reply;
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
            Mono<MediaResource> generatedImageMono
    ) {
        static StreamPlan text(List<MediaResource> attachments, Flux<String> replyFlux) {
            return new StreamPlan("TEXT", attachments, replyFlux, false, "", null);
        }

        static StreamPlan image(List<MediaResource> attachments, Flux<String> replyFlux, String imagePrompt, Mono<MediaResource> generatedImageMono) {
            return new StreamPlan("IMAGE", attachments, replyFlux, true, imagePrompt, generatedImageMono);
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

    public record Decision(boolean generateImage, String reply, String imagePrompt) {
        static Decision text(String reply) {
            return new Decision(false, reply == null ? "" : reply, "");
        }
    }

    static record ChatRequestContext(
            String message,
            List<MediaResource> attachments,
            String inlineAttachmentText
    ) {
        boolean hasAttachment() {
            return attachments != null && !attachments.isEmpty();
        }

        String attachmentSummary() {
            if (!hasAttachment()) {
                return "未上传附件";
            }
            MediaResource attachment = attachments.get(0);
            return "fileName=" + safeValue(attachment.fileName())
                    + ", contentType=" + safeValue(attachment.contentType())
                    + ", fileSize=" + attachment.fileSize();
        }

        String decisionAttachmentPreview() {
            if (inlineAttachmentText == null) {
                return "";
            }
            String normalized = inlineAttachmentText.trim();
            if (normalized.length() <= MAX_DECISION_ATTACHMENT_PREVIEW_CHARS) {
                return normalized;
            }
            return normalized.substring(0, MAX_DECISION_ATTACHMENT_PREVIEW_CHARS) + "\n...[truncated]";
        }

        private static String safeValue(String value) {
            return value == null || value.isBlank() ? "unknown" : value;
        }
    }
}
