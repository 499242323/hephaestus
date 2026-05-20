package com.example.springaidemo.controller;

import com.example.springaidemo.service.MultimodalChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultimodalChatController.class)
class MultimodalChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MultimodalChatService multimodalChatService;

    @Test
    void rejectsEmptyMessageAndMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/chat/multimodal")
                        .param("message", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入消息或上传附件"));
    }

    @Test
    void sendsTextMessageToUnifiedService() throws Exception {
        when(multimodalChatService.chat(eq("你好"), any(), eq("session-1")))
                .thenReturn(new MultimodalChatService.MultimodalResponse("TEXT", "你好，我在。", false, "", null, List.of(), null));

        mockMvc.perform(multipart("/api/chat/multimodal")
                        .param("message", "你好")
                        .header("X-Session-Id", "session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.reply").value("你好，我在。"))
                .andExpect(jsonPath("$.generateImage").value(false));

        verify(multimodalChatService).chat(eq("你好"), any(), eq("session-1"));
    }

    @Test
    void sendsAttachmentToUnifiedService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.bin",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[]{1, 2, 3}
        );

        when(multimodalChatService.chat(eq("分析这个附件"), any(), eq("session-2")))
                .thenReturn(new MultimodalChatService.MultimodalResponse(
                        "TEXT",
                        "附件已分析。",
                        false,
                        "",
                        null,
                        List.of(new MultimodalChatService.MediaResource(12L, "demo.bin", MediaType.APPLICATION_OCTET_STREAM_VALUE, 3, "/mediadl/media/getdata/rec/upload/20260519/session-2/abc/demo.bin", "/mediadl/media/getdata/rec/upload/20260519/session-2/abc/demo.bin")),
                        null
                ));

        mockMvc.perform(multipart("/api/chat/multimodal")
                .file(file)
                .param("message", "分析这个附件")
                .header("X-Session-Id", "session-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("附件已分析。"))
                .andExpect(jsonPath("$.attachments[0].url").value("/mediadl/media/getdata/rec/upload/20260519/session-2/abc/demo.bin"));
    }

    @Test
    void returnsImageResponseWhenServiceGeneratesImage() throws Exception {
        when(multimodalChatService.chat(eq("画一张图"), any(), eq("session-3")))
                .thenReturn(new MultimodalChatService.MultimodalResponse(
                        "IMAGE",
                        "已生成图片。",
                        true,
                        "画一张图",
                        "/mediadl/media/getdata/rec/down/20260519/session-3/xyz/generated-image.png",
                        List.of(),
                        new MultimodalChatService.MediaResource(13L, "generated-image.png", MediaType.IMAGE_PNG_VALUE, 3, "/mediadl/media/getdata/rec/down/20260519/session-3/xyz/generated-image.png", "/mediadl/media/getdata/rec/down/20260519/session-3/xyz/generated-image.png")
                ));

        mockMvc.perform(multipart("/api/chat/multimodal")
                        .param("message", "画一张图")
                        .header("X-Session-Id", "session-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("IMAGE"))
                .andExpect(jsonPath("$.reply").value("已生成图片。"))
                .andExpect(jsonPath("$.generateImage").value(true))
                .andExpect(jsonPath("$.imagePrompt").value("画一张图"))
                .andExpect(jsonPath("$.imageUrl").value("/mediadl/media/getdata/rec/down/20260519/session-3/xyz/generated-image.png"))
                .andExpect(jsonPath("$.generatedImage.url").value("/mediadl/media/getdata/rec/down/20260519/session-3/xyz/generated-image.png"));
    }
}
