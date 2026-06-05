package olympus.hephaestus.media.controller;

import olympus.hephaestus.media.domain.MediaFile;
import olympus.hephaestus.media.service.MediaFileService;
import olympus.hephaestus.media.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaFileController.class)
class MediaFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaFileService mediaFileService;

    @MockBean
    private MediaStorageService mediaStorageService;

    @Test
    void proxiesInlineFileContent() throws Exception {
        MediaFile mediaFile = mediaFile();
        when(mediaFileService.findById(12L)).thenReturn(Optional.of(mediaFile));
        when(mediaStorageService.read("rec/upload/20260519/session-1/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/demo-txt")).thenReturn("hello".getBytes());

        mockMvc.perform(get("/api/media/files/12"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"))
                .andExpect(header().string("Content-Type", MediaType.TEXT_PLAIN_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("inline")));
    }

    @Test
    void proxiesDownloadFileContent() throws Exception {
        MediaFile mediaFile = mediaFile();
        when(mediaFileService.findById(12L)).thenReturn(Optional.of(mediaFile));
        when(mediaStorageService.read("rec/upload/20260519/session-1/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/demo-txt")).thenReturn("hello".getBytes());

        mockMvc.perform(get("/api/media/files/12/download"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    void returnsNotFoundWhenMetadataMissing() throws Exception {
        when(mediaFileService.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/media/files/404"))
                .andExpect(status().isNotFound());
    }

    private static MediaFile mediaFile() {
        return new MediaFile(
                12L,
                "session-1",
                "demo.txt",
                "demo-txt",
                MediaType.TEXT_PLAIN_VALUE,
                5,
                "rec/upload/20260519/session-1/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/demo-txt",
                "/hephaestus/mediadl/media/getdata/rec/upload/20260519/session-1/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/demo-txt",
                "USER_UPLOAD",
                LocalDateTime.now()
        );
    }
}
