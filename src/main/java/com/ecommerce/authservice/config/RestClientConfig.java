package com.ecommerce.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate configuration for synchronous service-to-service communication.
 *
 * <p>Timeouts are intentionally conservative:
 * <ul>
 *   <li><b>connect timeout</b> — 3 000 ms: fail fast if the target service is not reachable.</li>
 *   <li><b>read timeout</b>    — 5 000 ms: enough for any normal downstream response, prevents
 *                                           hanging threads under slow network or GC pauses.</li>
 * </ul>
 *
 * <p>Uses {@link SimpleClientHttpRequestFactory} which is always available on the classpath
 * regardless of Spring Boot version — no optional HTTP client dependencies required.
 *
 * <p>When Kafka is re-enabled this bean becomes unused but harmless;
 * no code will break if it remains on the classpath.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000); // 3 seconds
        factory.setReadTimeout(5_000);    // 5 seconds
        return new RestTemplate(factory);
    }
}
