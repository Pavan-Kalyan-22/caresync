package com.caresync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class WeatherConfig {

    @Value("${weather.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${weather.read-timeout:5000}")
    private int readTimeout;

    @Bean
    public RestClient weatherRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
