package olympus.hephaestus;

import olympus.hephaestus.media.config.MediaStorageProperties;
import olympus.hephaestus.login.auth.config.LoginAuthProperties;
import olympus.hephaestus.login.auth.config.LoginPasswordCryptoProperties;
import olympus.hephaestus.login.log.config.LoginLogCleanupProperties;
import olympus.hephaestus.weather.tools.EmailProperties;
import olympus.hephaestus.weather.WeatherProperties;
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
