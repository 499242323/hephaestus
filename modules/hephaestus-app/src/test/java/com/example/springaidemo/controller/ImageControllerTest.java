package com.example.springaidemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void describeImageReturnsSimpleChineseDescription() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3, 4, 5}
        );

        mockMvc.perform(multipart("/api/images/describe")
                        .file(file)
                        .param("width", "640")
                        .param("height", "360"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("demo.png"))
                .andExpect(jsonPath("$.imageType").value(MediaType.IMAGE_PNG_VALUE))
                .andExpect(jsonPath("$.fileSize").value(5))
                .andExpect(jsonPath("$.width").value(640))
                .andExpect(jsonPath("$.height").value(360))
                .andExpect(jsonPath("$.description", containsString("这是一张 PNG 图片")))
                .andExpect(jsonPath("$.description", containsString("640 x 360")));
    }

    @Test
    void describeImageRejectsNonImageFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/images/describe").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请上传图片文件"));
    }

    @Test
    void generateImageReturnsDataUrl() throws Exception {
        mockMvc.perform(post("/api/images/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"生成一张 Hephaestus 聊天页面图片\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("生成一张 Hephaestus 聊天页面图片"))
                .andExpect(jsonPath("$.imageUrl", startsWith("data:image/svg+xml;base64,")))
                .andExpect(jsonPath("$.summary", containsString("已根据描述生成演示图片")));
    }

    @Test
    void generateImageRejectsBlankPrompt() throws Exception {
        mockMvc.perform(post("/api/images/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("图片描述不能为空"));
    }
}
