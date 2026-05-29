package com.example.springaidemo;

import com.example.springaidemo.media.config.MediaStorageProperties;
import com.example.springaidemo.login.auth.config.LoginAuthProperties;
import com.example.springaidemo.login.auth.config.LoginPasswordCryptoProperties;
import com.example.springaidemo.login.log.config.LoginLogCleanupProperties;
import com.example.springaidemo.weather.tools.EmailProperties;
import com.example.springaidemo.weather.WeatherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        MediaStorageProperties.class,
        LoginAuthProperties.class,
        LoginPasswordCryptoProperties.class,
        LoginLogCleanupProperties.class,
        WeatherProperties.class,
        EmailProperties.class
})
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }

}
