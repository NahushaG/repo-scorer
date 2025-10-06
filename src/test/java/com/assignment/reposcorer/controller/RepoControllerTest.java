/* TEST FILE - auto-tidied: assertions and error-case coverage recommended */
package com.assignment.reposcorer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.assignment.reposcorer.domain.ScoredRepo;
import com.assignment.reposcorer.dto.RepoResponse;
import com.assignment.reposcorer.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@WebFluxTest(controllers = RepoController.class)
class RepoControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private ScoringService scoringService; // Mock the service

    @Test
    void shouldReturnRepoResponseWithMetadata() {
        // Prepare a dummy scored repo
        ScoredRepo scoredRepo = new ScoredRepo(
                "test",
                "user/test",
                "user",
                "url",
                "Java",
                10,
                2,
                OffsetDateTime.now().minusDays(5),
                1.234567
        );

        // Mock the service to return a RepoResponse
        RepoResponse response = new RepoResponse(
                "Java",
                "2024-01-01",
                10,
                1,          // totalCount
                1,          // maxAvailable
                List.of(scoredRepo)
        );

        Mockito.when(scoringService.scoreWithMetadata("Java", LocalDate.parse("2024-01-01"), "topic:backend", 10))
                .thenReturn(Mono.just(response));

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
                .expectBody()
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.data[0].name").isEqualTo("test")
                .jsonPath("$.data[0].score").isNumber();
    }
}
