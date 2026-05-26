package com.example.springaidemo.weather.tools;

import com.example.springaidemo.weather.QWeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service("weatherServiceTool")
public class WeatherServiceTool {

    @Autowired
    private QWeatherService qWeatherService;

    @Tool(description = "获取指定城市今天的天气情况")
    public String getWeather(@ToolParam(description = "要查询天气的城市名称，例如北京、上海、广州") String city) {
        QWeatherService.TodayWeatherResult result = qWeatherService.getTodayWeather(city);
        String normalizedCity = result.city() == null || result.city().isBlank() ? "北京" : result.city().trim();
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
                """.formatted(safe(result.city()), safe(result.district()), safe(result.province()), safe(result.weatherText()), safe(result.temperature()), safe(result.feelsLike()), safe(result.windDirection()), safe(result.windScale()), safe(result.humidity()), safe(result.precipitation()), safe(result.observedAt()));
    }

    @Tool(description = "获取当前用户所在时区的时间")
    public String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未知" : value;
    }
}
