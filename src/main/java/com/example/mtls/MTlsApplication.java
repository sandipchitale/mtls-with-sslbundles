package com.example.mtls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@SpringBootApplication
public class MTlsApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MTlsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MTlsApplication.class, args);
    }

    @RestController
    static class IndexController {
        @GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
        String index() {
            return "index";
        }
    }

    @Bean
    CommandLineRunner clr(Clients client) {
        return (String... args) -> {

            // RestClient
            LOGGER.info("RestClient loopback");
            ResponseEntity<String> localhost = client.getRestClient().get().uri("https://localhost:8443").retrieve().toEntity(String.class);
            LOGGER.info("RestClient: Got {} from localhost", localhost.getStatusCode());

            LOGGER.info("RestClient 127.0.0.1 without loopback trust");
            try {
                ResponseEntity<String> loopback = client.getRestClient().get().uri("https://127.0.0.1:8443").retrieve().toEntity(String.class);
                LOGGER.info("RestClient 127.0.0.1: Got {} from 127.0.0.1", localhost.getStatusCode());
            } catch (Exception e) {
                LOGGER.error("Expected error for 127.0.0.1: " + e.getMessage());
            }

            LOGGER.info("RestClient 127.0.0.1 with loopback trust");
            ResponseEntity<String> loopback = client.getRestClientLoopback().get().uri("https://127.0.0.1:8443").retrieve().toEntity(String.class);
            LOGGER.info("RestClient 127.0.0.1 with loopback trust: Got {} from 127.0.0.1", localhost.getStatusCode());

            LOGGER.info("RestClient example.com");
            ResponseEntity<String> exampleCom = client.getRestClient().get().uri("https://example.com").retrieve().toEntity(String.class);
            LOGGER.info("RestClient: Got {} from example.com", exampleCom.getStatusCode());

            // RestTemplate
            LOGGER.info("RestTemplate localhost");
            localhost = client.getRestTemplate().getForEntity("https://localhost:8443", String.class);
            LOGGER.info("RestTemplate: Got {} from localhost", localhost.getStatusCode());

            LOGGER.info("RestTemplate 127.0.0.1 without loopback trust");
            try {
                loopback = client.getRestTemplate().getForEntity("https://127.0.0.1:8443", String.class);
                LOGGER.info("RestTemplate: Got {} from 127.0.0.1", loopback.getStatusCode());
            } catch (Exception e) {
                LOGGER.error("Expected error for 127.0.0.1: " + e.getMessage());
            }

            LOGGER.info("RestTemplate 127.0.0.1 with loopback trust");
            loopback = client.getRestTemplateLoopback().getForEntity("https://127.0.0.1:8443", String.class);
            LOGGER.info("RestTemplate 127.0.0.1 with loopback trust: Got {} from 127.0.0.1", loopback.getStatusCode());

            LOGGER.info("RestTemplate example.com");
            exampleCom = client.getRestTemplate().getForEntity("https://example.com", String.class);
            LOGGER.info("RestTemplate: Got {} from example.com", exampleCom.getStatusCode());

            LOGGER.info("WebClient localhost");
            localhost = client.getWebClient().get().uri("https://localhost:8443").retrieve().toEntity(String.class).block();
            LOGGER.info("WebClient: Got {} from localhost", Objects.requireNonNull(localhost).getStatusCode());

            LOGGER.info("WebClient example.com");
            exampleCom = client.getWebClient().get().uri("https://example.com").retrieve().toEntity(String.class).block();
            LOGGER.info("WebClient: Got {} from example.com", Objects.requireNonNull(exampleCom).getStatusCode());
        };
    }
}
