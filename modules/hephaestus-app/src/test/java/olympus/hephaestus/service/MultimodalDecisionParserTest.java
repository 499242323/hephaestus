package olympus.hephaestus.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultimodalDecisionParserTest {

    @Test
    void parsesStandardJsonDecision() {
        MultimodalChatService.Decision decision = MultimodalChatService.parseDecision(
                "{\"generateImage\":true,\"reply\":\"准备生成图片\",\"imagePrompt\":\"一张科技感海报\"}"
        );

        assertThat(decision.generateImage()).isTrue();
        assertThat(decision.reply()).isEqualTo("准备生成图片");
        assertThat(decision.imagePrompt()).isEqualTo("一张科技感海报");
    }

    @Test
    void parsesJsonFromMarkdownCodeBlock() {
        MultimodalChatService.Decision decision = MultimodalChatService.parseDecision(
                "```json\n"
                        + "{\n"
                        + "  \"generateImage\": false,\n"
                        + "  \"reply\": \"这是附件摘要\",\n"
                        + "  \"imagePrompt\": \"\"\n"
                        + "}\n"
                        + "```"
        );

        assertThat(decision.generateImage()).isFalse();
        assertThat(decision.reply()).isEqualTo("这是附件摘要");
        assertThat(decision.imagePrompt()).isEmpty();
    }

    @Test
    void fallsBackToTextWhenJsonCannotBeParsed() {
        MultimodalChatService.Decision decision = MultimodalChatService.parseDecision("这是一段普通回复，不是 JSON。");

        assertThat(decision.generateImage()).isFalse();
        assertThat(decision.reply()).isEqualTo("这是一段普通回复，不是 JSON。");
        assertThat(decision.imagePrompt()).isEmpty();
    }
}
