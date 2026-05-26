package com.example.springaidemo.controller;

import com.example.springaidemo.service.MultimodalStreamEvent;
import com.example.springaidemo.service.MultimodalStreamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class MultimodalChatStreamController {

    private final MultimodalStreamingService multimodalStreamingService;

    public MultimodalChatStreamController(MultimodalStreamingService multimodalStreamingService) {
        this.multimodalStreamingService = multimodalStreamingService;
    }

    @PostMapping(value = "/multimodal/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@ModelAttribute MultimodalChatStreamRequest request,
                                                @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        request.setSessionId(sessionId);
        return multimodalStreamingService.stream(
                        request.normalizeMessage(),
                        request.getFile(),
                        request.normalizeConversationId()
                )
                .map(this::toServerSentEvent);
    }

    private ServerSentEvent<String> toServerSentEvent(MultimodalStreamEvent event) {
        return ServerSentEvent.<String>builder()
                .event(event.event())
                .data(multimodalStreamingService.toJson(event.data()))
                .build();
    }
}
