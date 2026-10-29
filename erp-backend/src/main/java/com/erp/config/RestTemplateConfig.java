package com.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Tương tự như việc cấu hình Http Client (Guzzle / Http facade) trong Laravel.
 * Cung cấp RestTemplate Bean để gọi các dịch vụ API bên ngoài (như Google Gemini API).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6000);  // 6s connect timeout
        factory.setReadTimeout(12000);   // 12s read timeout -> nếu quá 12s tự động fallback ngay
        return new RestTemplate(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

