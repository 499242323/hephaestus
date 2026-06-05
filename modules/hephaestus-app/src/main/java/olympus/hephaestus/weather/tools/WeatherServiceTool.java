package olympus.hephaestus.weather.tools;

import olympus.hephaestus.weather.QWeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service("weatherServiceTool")
public class WeatherServiceTool {

    private final QWeatherService qWeatherService;

    public WeatherServiceTool(QWeatherService qWeatherService) {
        this.qWeatherService = qWeatherService;
    }

    @Tool(description = "获取指定城市今天的实时天气，例如北京、上海、长沙")
    public String getWeather(
            @ToolParam(description = "要查询天气的城市名称，例如北京、上海、长沙") String city) {
        return formatWeather(qWeatherService.getTodayWeather(city));
    }

    @Tool(description = "根据经纬度获取当前位置今天的实时天气")
    public String getWeatherByCoordinates(
            @ToolParam(description = "纬度，例如 39.92") String latitude,
            @ToolParam(description = "经度，例如 116.41") String longitude) {
        return formatWeather(qWeatherService.getTodayWeatherByCoordinates(latitude, longitude));
    }

    @Tool(description = "获取当前用户所在时区的当前时间")
    public String getCurrentDateTime() {
        return LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
    }

    private String formatWeather(QWeatherService.TodayWeatherResult result) {
        String normalizedCity = result.city() == null || result.city().isBlank() ? "当前位置" : result.city().trim();
        if (!result.available()) {
            return "暂时无法获取" + normalizedCity + "今天的实时天气，原因：" + result.message();
        }

        return """
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
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }
}
