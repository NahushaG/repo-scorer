/* TEST FILE - auto-tidied: assertions and error-case coverage recommended */
package com.assignment.reposcorer.integration;

import com.assignment.reposcorer.domain.GitHubRepo;
import com.assignment.reposcorer.dto.RepoResponse;
import com.assignment.reposcorer.service.RepoSearchClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RepoIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private RepoSearchClient client;

    private GitHubRepo sampleRepo;

    @BeforeEach
    void setup() {
        sampleRepo = new GitHubRepo(
                "test-repo",
                "user/test-repo",
                new GitHubRepo.Owner("user"),
                "https://github.com/user/test-repo",
                "Java",
                50,
                10,
                OffsetDateTime.now().minusDays(3)
        );
    }

    @Test
    void shouldReturnScoredReposWithValidScore() {
        // Mock search to return a Flux of sampleRepo
        when(client.search(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Flux.just(sampleRepo));

        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/repos")
                        .queryParam("language", "Java")
                        .queryParam("since", "2024-01-01")
                        .queryParam("q", "topic:backend")
                        .queryParam("limit", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(RepoResponse.class)
                .consumeWith(response -> {
                    RepoResponse body = response.getResponseBody();
                    Assertions.assertNotNull(body, "Response body should not be null");
                    Assertions.assertNotNull(body.data(), "Repo data should not be null");
                    Assertions.assertEquals(1, body.data().size(), "There should be exactly 1 repo");
                    Assertions.assertEquals(sampleRepo.name(), body.data().get(0).name(), "Repo name should match");
                });
    }


    @Test
    void shouldReturnEmptyListWhenNoReposFound() {
        when(client.search(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Flux.empty());

        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/repos")
                        .queryParam("language", "Python")
                        .queryParam("since", "2025-01-01")
                        .queryParam("q", "topic:backend")
                        .queryParam("limit", "5")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data").isEmpty()
                .jsonPath("$.count").isEqualTo(0)
                .jsonPath("$.total").isEqualTo(0);
    }
}
