package com.example.springaidemo.media;

import com.example.springaidemo.media.config.MediaStorageProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MediaStorageProperties.class)
public class MediaTestApplication {
}
