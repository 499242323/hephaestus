package com.example.springaidemo.mybatis.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
@MapperScan({
        "com.example.springaidemo.media.repository",
        "com.example.springaidemo.chatmemory.repository",
        "com.example.springaidemo.org.repository"
})
public class HephaestusMybatisAutoConfiguration {
}
