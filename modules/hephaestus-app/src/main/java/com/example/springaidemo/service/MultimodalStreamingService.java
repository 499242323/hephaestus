package com.example.springaidemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MultimodalStreamingService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MultimodalChatService multimodalChatService;

    public MultimodalStreamingService(MultimodalChatService multimodalChatService) {
        this.multimodalChatService = multimodalChatService;
    }

    public Flux<MultimodalStreamEvent> stream(String message, MultipartFile file, String conversationId) {
        return Flux.defer(() -> {
            MultimodalChatService.StreamPlan plan = multimodalChatService.prepareStream(message, file, conversationId);
            Flux<MultimodalStreamEvent> textFlow = Flux.concat(
                    Flux.just(status("analyzing", file == null || file.isEmpty() ? "正在整理回复" : "正在分析附件")),
                    attachmentEvents(plan.attachments()),
                    deltaEvents(plan.replyFlux())
            );

            Flux<MultimodalStreamEvent> imageFlow = plan.generateImage()
                    ? Flux.concat(
                    Flux.just(status("image_generating", "正在生成图片")),
                    plan.generatedImageMono()
                            .map(this::image)
                            .flux()
                            .onErrorResume(exception -> {
                                log.error("图片生成或保存失败，conversationId={}", conversationId, exception);
                                return Flux.just(error("图片生成失败：" + exception.getMessage()));
                            })
            ) : Flux.empty();

            Flux<MultimodalStreamEvent> combinedFlow = plan.generateImage()
                    ? Flux.merge(textFlow, imageFlow)
                    : textFlow;

            return Flux.mergeSequential(
                    combinedFlow,
                    Flux.just(done(plan.type(), plan.generateImage()))
            );
        }).onErrorResume(exception -> {
            log.error("多模态流式请求处理失败，conversationId={}, hasFile={}, fileName={}",
                    conversationId,
                    file != null && !file.isEmpty(),
                    file == null ? null : file.getOriginalFilename(),
                    exception);
            return Flux.just(error(exception.getMessage()));
        });
    }

    private Flux<MultimodalStreamEvent> attachmentEvents(List<MultimodalChatService.MediaResource> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Flux.empty();
        }
        return Flux.just(new MultimodalStreamEvent("attachments", Map.of("attachments", attachments)));
    }

    private Flux<MultimodalStreamEvent> deltaEvents(Flux<String> replyFlux) {
        if (replyFlux == null) {
            return Flux.empty();
        }
        return replyFlux
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .map(chunk -> new MultimodalStreamEvent("delta", Map.of("content", chunk)));
    }

    private MultimodalStreamEvent status(String phase, String message) {
        return new MultimodalStreamEvent("status", Map.of("phase", phase, "message", message));
    }

    private MultimodalStreamEvent image(MultimodalChatService.MediaResource generatedImage) {
        return new MultimodalStreamEvent("image", Map.of("generatedImage", generatedImage));
    }

    private MultimodalStreamEvent done(String type, boolean generateImage) {
        return new MultimodalStreamEvent("done", Map.of("type", type, "generateImage", generateImage));
    }

    private MultimodalStreamEvent error(String message) {
        return new MultimodalStreamEvent("error", Map.of("message", message == null || message.isBlank() ? "处理请求时发生错误。" : message));
    }

    public String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("事件序列化失败", exception);
        }
    }
}
