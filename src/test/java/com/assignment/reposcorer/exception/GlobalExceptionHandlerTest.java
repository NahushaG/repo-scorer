/* TEST FILE - auto-tidied: assertions and error-case coverage recommended */
package com.assignment.reposcorer.exception;

import com.assignment.reposcorer.controller.RepoController;
import com.assignment.reposcorer.domain.ScoredRepo;
import com.assignment.reposcorer.dto.RepoResponse;
import com.assignment.reposcorer.service.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.web.reactive.function.client.WebClientResponseException.create;

@WebFluxTest(controllers = RepoController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ScoringService scoringService;

    @BeforeEach
    void setup() {
        // default mock: successful response
        Mockito.when(scoringService.scoreWithMetadata(any(), any(), any(), anyInt()))
                .thenReturn(getDummyResponse());
    }

    @Test
    @DisplayName("should return 400 for invalid request parameter")
    void shouldReturn400ForInvalidParameter() {
        webTestClient.get()
                .uri("/api/v1/repos?language=&since=2024-01-01")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/api/v1/repos");
    }

    @Test
    @DisplayName("should handle WebClientResponseException gracefully")
    void shouldHandleWebClientError() {
        WebClientResponseException ex = create(
                403, "Forbidden", null, "Rate limit exceeded".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        );

        Mockito.when(scoringService.scoreWithMetadata(any(), any(), any(), anyInt()))
                .thenReturn(Mono.error(ex));

        webTestClient.get()
                .uri("/api/v1/repos?language=java&since=2024-01-01")
                .exchange()
                .expectStatus().isEqualTo(403)
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").value(msg -> msg.toString().contains("Forbidden"))
                .jsonPath("$.path").isEqualTo("/api/v1/repos");
    }

    @Test
    @DisplayName("should handle generic exception as 500")
    void shouldHandleGenericException() {
        Mockito.when(scoringService.scoreWithMetadata(any(), any(), any(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Unexpected failure")));

        webTestClient.get()
                .uri("/api/v1/repos?language=java&since=2024-01-01")
                .exchange()
                .expectStatus().is5xxServerError() // Expect 500
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.message").value(msg -> ((String) msg).contains("Unexpected failure"))
                .jsonPath("$.path").isEqualTo("/api/v1/repos");
    }


    public Mono<RepoResponse> getDummyResponse() {
        String dateString = "2024-01-01";
        LocalDate date = LocalDate.parse(dateString);
        OffsetDateTime odt = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        RepoResponse dummy = new RepoResponse(
                "Test",
                dateString,
                5,
                1,
                1,
                List.of(
                        new ScoredRepo("Test Name",
                                "Test Full Name",
                                "Test Owner",
                                "Test URL",
                                "Test Language",
                                1,
                                1,
                                odt,
                                24)
                )
        );
        return Mono.just(dummy);
    }
}
