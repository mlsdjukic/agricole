package com.example.alarms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonbConverter implements Converter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper;

    public JsonbConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convert(Map<String, Object> source) {
        try {
            return objectMapper.writeValueAsString(source);  // Convert Map to JSON String
        } catch (Exception e) {
            throw new IllegalArgumentException("Error serializing Map to JSON", e);
        }
    }
}