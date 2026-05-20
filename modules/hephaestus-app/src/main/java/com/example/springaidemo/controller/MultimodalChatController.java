package com.example.springaidemo.controller;

import com.example.springaidemo.service.MultimodalChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class MultimodalChatController {

    private final MultimodalChatService multimodalChatService;

    public MultimodalChatController(MultimodalChatService multimodalChatService) {
        this.multimodalChatService = multimodalChatService;
    }

    @PostMapping("/multimodal")
    public MultimodalChatService.MultimodalResponse multimodalChat(@RequestParam(value = "message", required = false) String message,
                                                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                                                   @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.isBlank() && (file == null || file.isEmpty())) {
            throw new BadRequestException("请输入消息或上传附件");
        }

        String conversationId = sessionId == null || sessionId.isBlank() ? "default" : sessionId;
        return multimodalChatService.chat(normalizedMessage, file, conversationId);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException exception) {
        log.error("媒体存储异常: {}", exception.getMessage(), exception);
        return Map.of("message", exception.getMessage());
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
