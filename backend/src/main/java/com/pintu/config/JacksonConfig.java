package com.pintu.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.pintu.common.util.TimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps every LocalDateTime request and response on the public API contract.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(TimeFormatter.PATTERN));
            builder.deserializers(new LocalDateTimeDeserializer(TimeFormatter.PATTERN));
        };
    }
}
