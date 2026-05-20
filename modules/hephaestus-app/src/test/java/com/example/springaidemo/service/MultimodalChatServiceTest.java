package com.example.springaidemo.service;

import com.example.springaidemo.media.MediaFile;
import com.example.springaidemo.media.MediaFileRepository;
import com.example.springaidemo.media.MediaStorageException;
import com.example.springaidemo.media.MediaStorageProperties;
import com.example.springaidemo.media.MediaStorageService;
import com.example.springaidemo.media.StoredMediaFile;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MultimodalChatServiceTest {

    @Test
    void returnsRemoteImageUrlWhenImageStorageDownloadFails() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec responseSpec = mock(ChatClient.StreamResponseSpec.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ImageModel> imageModelProvider = mock(ObjectProvider.class);
        ImageModel imageModel = mock(ImageModel.class);
        ImageResponse imageResponse = mock(ImageResponse.class, Answers.RETURNS_DEEP_STUBS);
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        MediaFileRepository mediaFileRepository = mock(MediaFileRepository.class);
        MediaStorageProperties mediaStorageProperties = mediaStorageProperties();

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(Flux.just(
                "{\n"
                        + "  \"generateImage\": true,\n"
                        + "  \"reply\": \"好的，我来为你生成一张懒惰的猫咪图片。\",\n"
                        + "  \"imagePrompt\": \"一只懒惰的猫咪躺在沙发上睡觉，温暖午后，插画风格\"\n"
                        + "}"
        ));
        when(imageModelProvider.getIfAvailable()).thenReturn(imageModel);
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse);
        when(imageResponse.getResult().getOutput().getUrl()).thenReturn("http://example.com/generated/cat.png");
        when(mediaStorageService.uploadImageFromUrl(eq("session-404"), eq("http://example.com/generated/cat.png")))
                .thenThrow(new MediaStorageException("404 Not Found"));

        MultimodalChatService service = new MultimodalChatService(
                chatClient,
                imageModelProvider,
                mediaStorageService,
                mediaFileRepository,
                mediaStorageProperties
        );

        MultimodalChatService.MultimodalResponse response = service.chat("画一只懒惰的猫咪", null, "session-404");

        assertThat(response.type()).isEqualTo("IMAGE");
        assertThat(response.generateImage()).isTrue();
        assertThat(response.reply()).isEqualTo("好的，我来为你生成一张懒惰的猫咪图片。");
        assertThat(response.imageUrl()).isEqualTo("http://example.com/generated/cat.png");
        assertThat(response.generatedImage()).isNotNull();
        assertThat(response.generatedImage().url()).isEqualTo("http://example.com/generated/cat.png");
        assertThat(response.generatedImage().downloadUrl()).isEqualTo("http://example.com/generated/cat.png");
        verifyNoInteractions(mediaFileRepository);
    }

    @Test
    void fallsBackToInlineImageWhenImageModelFails() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec responseSpec = mock(ChatClient.StreamResponseSpec.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ImageModel> imageModelProvider = mock(ObjectProvider.class);
        ImageModel imageModel = mock(ImageModel.class);
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        MediaFileRepository mediaFileRepository = mock(MediaFileRepository.class);
        MediaStorageProperties mediaStorageProperties = mediaStorageProperties();
        StoredMediaFile storedMediaFile = new StoredMediaFile(
                "generated-image.svg",
                "generated-image.svg",
                MediaType.IMAGE_PNG_VALUE,
                128L,
                "rec/down/20260519/session-inline/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/generated-image.svg",
                new byte[0]
        );

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(Flux.just(
                "{\n"
                        + "  \"generateImage\": true,\n"
                        + "  \"reply\": \"可以，帮你生成一张金鱼在海里的图片。\",\n"
                        + "  \"imagePrompt\": \"一条金鱼在海里游动，光线柔和，插画风格\"\n"
                        + "}"
        ));
        when(imageModelProvider.getIfAvailable()).thenReturn(imageModel);
        when(imageModel.call(any(ImagePrompt.class))).thenThrow(new RuntimeException("404 Not Found"));
        when(mediaStorageService.uploadImageFromDataUrl(eq("session-inline"), anyString())).thenReturn(storedMediaFile);

        MediaFile savedFile = new MediaFile(
                77L,
                "session-inline",
                "generated-image.svg",
                "generated-image.svg",
                MediaType.IMAGE_PNG_VALUE,
                128L,
                "rec/down/20260519/session-inline/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/generated-image.svg",
                "",
                "AI_GENERATED_IMAGE",
                null
        );
        when(mediaFileRepository.save(any(MediaFile.class))).thenReturn(savedFile);

        MultimodalChatService service = new MultimodalChatService(
                chatClient,
                imageModelProvider,
                mediaStorageService,
                mediaFileRepository,
                mediaStorageProperties
        );

        MultimodalChatService.MultimodalResponse response = service.chat("画一条在海里的金鱼", null, "session-inline");

        assertThat(response.type()).isEqualTo("IMAGE");
        assertThat(response.generateImage()).isTrue();
        assertThat(response.reply()).isEqualTo("可以，帮你生成一张金鱼在海里的图片。");
        assertThat(response.imageUrl()).isEqualTo("/mediadl/media/getdata/rec/down/20260519/session-inline/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/generated-image.svg");
        assertThat(response.generatedImage()).isNotNull();
        assertThat(response.generatedImage().url()).isEqualTo("/mediadl/media/getdata/rec/down/20260519/session-inline/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/generated-image.svg");
        assertThat(response.generatedImage().downloadUrl()).isEqualTo("/mediadl/media/getdata/rec/down/20260519/session-inline/9f645494-9e1b-4b82-8fd4-b9f1f11bf51d/generated-image.svg");
    }

    private static MediaStorageProperties mediaStorageProperties() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setAccessPathPrefix("/mediadl/media/getdata");
        return properties;
    }
}
