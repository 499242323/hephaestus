package olympus.hephaestus.media.controller;

import olympus.hephaestus.media.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaAccessController.class)
class MediaAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaStorageService mediaStorageService;

    @Test
    void proxiesMediaByStoragePath() throws Exception {
        when(mediaStorageService.read("rec/upload/20260519/session-1/abc/demo.png")).thenReturn("png".getBytes());

        mockMvc.perform(get("/mediadl/media/getdata/rec/upload/20260519/session-1/abc/demo.png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("png".getBytes()))
                .andExpect(header().string("Content-Type", "image/png"));
    }
}
