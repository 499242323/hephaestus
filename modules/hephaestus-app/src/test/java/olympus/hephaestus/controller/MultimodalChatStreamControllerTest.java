package olympus.hephaestus.controller;

import olympus.hephaestus.service.MultimodalStreamEvent;
import olympus.hephaestus.service.MultimodalStreamingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultimodalChatStreamController.class)
class MultimodalChatStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MultimodalStreamingService multimodalStreamingService;

    @Test
    void returnsSseEventStream() throws Exception {
        when(multimodalStreamingService.stream(eq("你好"), any(), eq("session-1")))
                .thenReturn(Flux.just(
                        new MultimodalStreamEvent("status", Map.of("phase", "analyzing", "message", "正在分析附件")),
                        new MultimodalStreamEvent("delta", Map.of("content", "你好"))
                ));
        when(multimodalStreamingService.toJson(any()))
                .thenReturn("{\"ok\":true}");

        var mvcResult = mockMvc.perform(multipart("/api/chat/multimodal/stream")
                        .param("message", "你好")
                        .header("X-Session-Id", "session-1"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:status")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:delta")));
    }
}
