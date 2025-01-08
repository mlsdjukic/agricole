package com.example.alarms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;

@Configuration
public class R2dbcConfig {

    @Bean
    public JsonbConverter jsonbConverter(ObjectMapper objectMapper) {
        return new JsonbConverter(objectMapper);
    }

    @Bean
    public GenericConversionService genericConversionService(JsonbConverter jsonbConverter) {
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(jsonbConverter);
        return conversionService;
    }
}
