package olympus.hephaestus.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @PostMapping(value = "/describe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageDescriptionResponse describeImage(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "width", required = false) Integer width,
                                                  @RequestParam(value = "height", required = false) Integer height) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("图片文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("请上传图片文件");
        }

        String fileName = file.getOriginalFilename() == null ? "未命名图片" : file.getOriginalFilename();
        String imageLabel = imageLabel(contentType);
        String dimensionText = width != null && height != null && width > 0 && height > 0
                ? "，图片尺寸约为 " + width + " x " + height
                : "，未读取到图片尺寸";
        String description = "这是一张 " + imageLabel + "，文件名为“" + fileName + "”，大小约 "
                + formatSize(file.getSize()) + dimensionText + "。";

        return new ImageDescriptionResponse(fileName, contentType, file.getSize(), width, height, description);
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImageGenerationResponse generateImage(@RequestBody ImageGenerationRequest request) {
        String prompt = request == null ? "" : trimToEmpty(request.getPrompt());
        if (prompt.isBlank()) {
            throw new BadRequestException("图片描述不能为空");
        }

        String imageUrl = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(buildSvg(prompt).getBytes(StandardCharsets.UTF_8));
        return new ImageGenerationResponse(prompt, imageUrl, "已根据描述生成演示图片：" + prompt);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(BadRequestException exception) {
        return Map.of("message", exception.getMessage());
    }

    private static String imageLabel(String contentType) {
        return switch (contentType.toLowerCase()) {
            case MediaType.IMAGE_PNG_VALUE -> "PNG 图片";
            case MediaType.IMAGE_JPEG_VALUE -> "JPEG 图片";
            case MediaType.IMAGE_GIF_VALUE -> "GIF 图片";
            case "image/webp" -> "WebP 图片";
            case "image/svg+xml" -> "SVG 图片";
            default -> contentType + " 图片";
        };
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private static String buildSvg(String prompt) {
        int hash = Math.abs(prompt.hashCode());
        String colorA = String.format("#%06x", 0x335577 + hash % 0x555555);
        String colorB = String.format("#%06x", 0x668855 + (hash / 7) % 0x444444);
        String safePrompt = escapeXml(prompt.length() > 48 ? prompt.substring(0, 48) + "..." : prompt);

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="960" height="540" viewBox="0 0 960 540">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="960" height="540" fill="url(#bg)"/>
                  <circle cx="760" cy="120" r="72" fill="rgba(255,255,255,0.22)"/>
                  <rect x="96" y="110" width="768" height="320" rx="28" fill="rgba(255,255,255,0.82)"/>
                  <text x="140" y="210" fill="#1f2937" font-family="Microsoft YaHei, Arial" font-size="38" font-weight="700">Spring AI Image</text>
                  <text x="140" y="280" fill="#374151" font-family="Microsoft YaHei, Arial" font-size="28">%s</text>
                  <text x="140" y="348" fill="#6b7280" font-family="Microsoft YaHei, Arial" font-size="22">Demo generated image</text>
                </svg>
                """.formatted(colorA, colorB, safePrompt);
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static class ImageGenerationRequest {
        private String prompt;

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }

    public record ImageDescriptionResponse(
            String fileName,
            String imageType,
            long fileSize,
            Integer width,
            Integer height,
            String description
    ) {
    }

    public record ImageGenerationResponse(String prompt, String imageUrl, String summary) {
    }

    private static class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
