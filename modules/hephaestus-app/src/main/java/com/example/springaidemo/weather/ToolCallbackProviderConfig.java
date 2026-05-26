package com.example.springaidemo.weather;

import com.example.springaidemo.weather.tools.EmailTools;
import com.example.springaidemo.weather.tools.WeatherServiceTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(WeatherServiceTool weatherServiceTool, EmailTools emailTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherServiceTool)
                .toolObjects(emailTools)
                .build();
    }
}
