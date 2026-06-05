package olympus.hephaestus.controller;

import olympus.hephaestus.chat.AiService;
import olympus.hephaestus.weather.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final WeatherService weatherService;

    public AiController(AiService aiService, WeatherService weatherService) {
        this.aiService = aiService;
        this.weatherService = weatherService;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request,
                       @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String conversationId = normalizeConversationId(sessionId);
        return aiService.chat(request.getMessage(), conversationId);
    }

    @GetMapping("/getchat")
    public String getchat(ChatRequest request,
                          @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String conversationId = normalizeConversationId(sessionId);
        return aiService.chat(request.getMessage(), conversationId);
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamChat(@RequestBody ChatRequest request,
                                   @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String conversationId = normalizeConversationId(sessionId);
        return aiService.streamChat(request.getMessage(), conversationId);
    }

    @GetMapping("/weather/today")
    public String todayWeather(@RequestParam(value = "city", required = false) String city,
                               @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String conversationId = normalizeConversationId(sessionId);
        return weatherService.todayWeather(city, conversationId);
    }

    private String normalizeConversationId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "default" : sessionId;
    }

    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
