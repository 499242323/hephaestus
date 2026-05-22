package com.example.springaidemo.media.service.impl;

import com.example.springaidemo.media.config.MediaStorageProperties;
import com.example.springaidemo.media.domain.StoredMediaFile;
import com.example.springaidemo.media.exception.MediaStorageException;
import com.example.springaidemo.media.service.MediaStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class MediaStorageServiceImpl implements MediaStorageService {

    private static final String WRITE_PATH = "/home/httpfile/writefile.htm?path=";
    private static final String READ_PATH = "/home/httpfile/readfile.htm?path=";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final MediaStorageProperties properties;
    private final RestClient restClient;

    public MediaStorageServiceImpl(MediaStorageProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public StoredMediaFile upload(String conversationId, MultipartFile file) {
        try {
            String originalFilename = normalizeOriginalFilename(file.getOriginalFilename(), "attachment.bin");
            String contentType = contentType(file.getContentType());
            return upload(conversationId, originalFilename, contentType, file.getBytes(), "upload");
        } catch (IOException exception) {
            log.error("读取上传附件失败: 会话ID={}, 附件名={}", conversationId, file.getOriginalFilename(), exception);
            throw new MediaStorageException("读取上传附件失败", exception);
        }
    }

    @Override
    public StoredMediaFile upload(String conversationId, String originalFilename, String contentType, byte[] bytes, String category) {
        String normalizedOriginalFilename = normalizeOriginalFilename(originalFilename, "file.bin");
        String storedFilename = toStoredFilename(normalizedOriginalFilename, "file.bin");
        String normalizedContentType = contentType(contentType);
        String storagePath = buildStoragePath(conversationId, category, storedFilename);
        URI requestUri = uploadUri(storagePath);
        String result = restClient.post()
                .uri(requestUri)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes)
                .retrieve()
                .body(String.class);
        if (!"0".equals(result == null ? "" : result.trim())) {
            throw new MediaStorageException("多媒体服务上传失败，返回：" + result);
        }
        return new StoredMediaFile(normalizedOriginalFilename, storedFilename, normalizedContentType, bytes.length, storagePath, bytes);
    }

    @Override
    public byte[] read(String storagePath) {
        URI requestUri = readUri(storagePath);
        byte[] bytes = restClient.post()
                .uri(requestUri)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .retrieve()
                .body(byte[].class);
        if (bytes == null) {
            throw new MediaStorageException("多媒体服务读取文件为空");
        }
        return bytes;
    }

    @Override
    public StoredMediaFile uploadImageFromDataUrl(String conversationId, String dataUrl) {
        int commaIndex = dataUrl == null ? -1 : dataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new MediaStorageException("图片 base64 数据格式不正确");
        }
        String meta = dataUrl.substring(0, commaIndex);
        String base64 = dataUrl.substring(commaIndex + 1);
        String contentType = meta.startsWith("data:") && meta.contains(";")
                ? meta.substring(5, meta.indexOf(';'))
                : MediaType.IMAGE_PNG_VALUE;
        byte[] bytes = Base64.getDecoder().decode(base64);
        return upload(conversationId, "generated-image." + extension(contentType), contentType, bytes, "down");
    }

    @Override
    public StoredMediaFile uploadImageFromUrl(String conversationId, String imageUrl) {
        byte[] bytes = restClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new MediaStorageException("下载生成图片失败");
        }
        return upload(conversationId, conversationId+".png", MediaType.IMAGE_PNG_VALUE, bytes, "down");
    }

    public static String toStoredFilename(String originalFilename, String fallback) {
        String normalized = normalizeOriginalFilename(originalFilename, fallback);
        String fileExtension = "";
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < normalized.length() - 1) {
            fileExtension = normalized.substring(dotIndex).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
            normalized = normalized.substring(0, dotIndex);
        }

        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "")
                .toLowerCase(Locale.ROOT);

        if (ascii.isBlank()) {
            ascii = fallback.contains(".") ? fallback.substring(0, fallback.indexOf('.')) : fallback;
            ascii = ascii.replaceAll("[^a-zA-Z0-9]+", "-").toLowerCase(Locale.ROOT);
        }
        if (ascii.isBlank()) {
            ascii = "file";
        }
        return ascii + fileExtension;
    }

    public static String toSessionPath(String conversationId) {
        String normalized = conversationId == null || conversationId.isBlank() ? "default" : conversationId.trim();
        normalized = normalized.replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? "default" : normalized;
    }

    private String buildStoragePath(String conversationId, String category, String storedFilename) {
        String prefix = trimSlashes(properties.getStoragePrefix());
        String normalizedCategory = normalizeCategory(category);
        String day = LocalDate.now().format(DAY_FORMATTER);
        String sessionPath = toSessionPath(conversationId);
        String folder = UUID.randomUUID().toString();
        return prefix + "/" + normalizedCategory + "/" + day + "/" + sessionPath + "/" + folder + "/" + storedFilename;
    }

    private String basicAuthHeader() {
        String token = properties.getUsername() + ":" + properties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private URI uploadUri(String storagePath) {
        return URI.create(baseUrl() + WRITE_PATH + UriUtils.encode(storagePath, StandardCharsets.UTF_8));
    }

    private URI readUri(String storagePath) {
        return URI.create(baseUrl() + READ_PATH + UriUtils.encode(storagePath, StandardCharsets.UTF_8));
    }

    private String baseUrl() {
        String baseUrl = properties.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String normalizeOriginalFilename(String filename, String fallback) {
        String value = filename == null || filename.isBlank() ? fallback : filename;
        String safe = value.replace('\\', '/');
        int slashIndex = safe.lastIndexOf('/');
        if (slashIndex >= 0) {
            safe = safe.substring(slashIndex + 1);
        }
        safe = safe.replaceAll("[\\r\\n\\t]", "_").trim();
        return safe.isBlank() ? fallback : safe;
    }

    private static String contentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;
    }

    private static String trimSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "rec" : normalized;
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if ("down".equals(normalized)) {
            return "down";
        }
        return "upload";
    }

    private static String extension(String contentType) {
        if (MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        if (MediaType.IMAGE_GIF_VALUE.equalsIgnoreCase(contentType)) {
            return "gif";
        }
        if (MimeTypeUtils.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)) {
            return "png";
        }
        String subtype = contentType.substring(contentType.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        return subtype.replaceAll("[^a-z0-9]", "");
    }
}
