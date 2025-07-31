package com.talktrip.talktrip.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.global.s3.MultipartJackson2HttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public WebConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new MultipartJackson2HttpMessageConverter(objectMapper));
    }
}

