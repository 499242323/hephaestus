package com.example.springaidemo.controller;

import com.example.springaidemo.chat.AiService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return aiService.chat(request.getMessage());
    }

    @GetMapping("/getchat")
    public String getchat( ChatRequest request) {
        return aiService.chat(request.getMessage());
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return aiService.streamChat(request.getMessage());
    }

    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}