package com.example.springaidemo.mybatis.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
@MapperScan("com.example.springaidemo.media.repository")
public class HephaestusMybatisAutoConfiguration {
}
