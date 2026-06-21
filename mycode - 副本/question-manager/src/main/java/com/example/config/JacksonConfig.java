package com.example.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON序列化配置
 * <p>
 * 统一配置时间格式，解决LocalDateTime返回带"T"的问题
 * 例如：2026-04-07T14:30:00 → 2026-04-07 14:30:00
 */
@Configuration
public class JacksonConfig {

    /**
     * 统一的时间格式：yyyy-MM-dd HH:mm:ss
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 自定义Jackson ObjectMapper配置
     * <p>
     * 全局配置LocalDateTime的序列化和反序列化格式
     * 所有返回给前端的LocalDateTime都会自动格式化
     *
     * @return Jackson2ObjectMapperBuilderCustomizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 配置序列化（Java对象 → JSON）
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));

            // 配置反序列化（JSON → Java对象）
            builder.deserializerByType(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        };
    }
}
