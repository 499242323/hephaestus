package com.example.springaidemo.media;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MediaStorageServiceTest {

    @Test
    void uploadsBytesWithBasicAuth() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MediaStorageService service = new MediaStorageService(properties(), builder);
        String token = Base64.getEncoder().encodeToString("egovahttp:egovahttp".getBytes());

        server.expect(once(), requestTo(startsWith("http://localhost:18080/HttpFileServer/home/httpfile/writefile.htm?path=rec%2F")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic " + token))
                .andRespond(withSuccess("0", MediaType.TEXT_PLAIN));

        StoredMediaFile stored = service.upload("session-1", "方案说明.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes(), "upload");

        assertThat(stored.originalFilename()).isEqualTo("方案说明.txt");
        assertThat(stored.storedFilename()).isNotBlank();
        assertThat(stored.contentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(stored.fileSize()).isEqualTo(5);
        assertThat(stored.storagePath()).startsWith("rec/");
        assertThat(stored.storagePath()).contains("/session-1/");
        server.verify();
    }

    @Test
    void failsWhenUploadResultIsNotZero() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MediaStorageService service = new MediaStorageService(properties(), builder);

        server.expect(once(), requestTo(startsWith("http://localhost:18080/HttpFileServer/home/httpfile/writefile.htm?path=")))
                .andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> service.upload("session-1", "demo.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes(), "upload"))
                .isInstanceOf(MediaStorageException.class)
                .hasMessageContaining("多媒体服务上传失败");
        server.verify();
    }

    @Test
    void convertsOriginalFilenameToSafeStoredFilename() {
        assertThat(MediaStorageService.toStoredFilename("需求说明(最终版).docx", "file.bin"))
                .endsWith(".docx");
        assertThat(MediaStorageService.toSessionPath("session 中文 01"))
                .isEqualTo("session-01");
    }

    private static MediaStorageProperties properties() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setBaseUrl("http://localhost:18080/HttpFileServer/");
        properties.setUsername("egovahttp");
        properties.setPassword("egovahttp");
        properties.setStoragePrefix("rec");
        return properties;
    }
}
