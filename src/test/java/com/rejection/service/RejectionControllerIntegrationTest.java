package com.rejection.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.security.user.name=test",
    "spring.security.user.password=test"
})
class RejectionControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getRejection_ShouldReturnValidResponse() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/rejection", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("reason");
        assertThat(response.getBody()).contains("timestamp");
        assertThat(response.getHeaders().get("X-Request-ID")).isNotNull();
    }

    @Test
    void getHealth_ShouldReturnUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("status");
    }
}