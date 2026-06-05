package olympus.hephaestus.weather;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;
    private final QWeatherService qWeatherService;

    public WeatherService(ChatClient chatClient, QWeatherService qWeatherService) {
        this.chatClient = chatClient;
        this.qWeatherService = qWeatherService;
    }

    public String todayWeather(String city, String conversationId) {
        QWeatherService.TodayWeatherResult result = qWeatherService.getTodayWeather(city);
        String normalizedCity = result.city() == null || result.city().isBlank() ? "北京" : result.city().trim();
        if (!result.available()) {
            return "暂时无法获取" + normalizedCity + "今天的实时天气，原因：" + result.message();
        }

        String weatherFacts = """
                城市：%s
                区县：%s
                省份：%s
                天气：%s
                当前温度：%s°C
                体感温度：%s°C
                风向：%s
                风力：%s级
                湿度：%s%%
                降水量：%s mm
                观测时间：%s
                """.formatted(
                safe(result.city()),
                safe(result.district()),
                safe(result.province()),
                safe(result.weatherText()),
                safe(result.temperature()),
                safe(result.feelsLike()),
                safe(result.windDirection()),
                safe(result.windScale()),
                safe(result.humidity()),
                safe(result.precipitation()),
                safe(result.observedAt())
        );

        return chatClient.prompt()
                .system("""
                        你是一个天气问答助手。
                        你会收到结构化的实时天气数据。
                        请基于给定数据，用中文输出简洁、自然、准确的天气说明。
                        不要虚构额外数据，不要补充未提供的预报信息。
                        """)
                .user("请根据以下实时天气数据，告诉我今天" + normalizedCity + "的天气情况：\n" + weatherFacts)
                .advisors(advisor -> advisor.param(CONVERSATION_ID_KEY, conversationId))
                .call()
                .content();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }
}
