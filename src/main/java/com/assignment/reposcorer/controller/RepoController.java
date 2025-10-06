package com.assignment.reposcorer.controller;

import com.assignment.reposcorer.dto.RepoResponse;
import com.assignment.reposcorer.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * REST controller for fetching and scoring GitHub repositories.
 * <p>
 * Supports filtering by language, creation date, and optional query parameters.
 * Returns a list of repositories ranked by a computed repository score.
 */
@RestController
@RequestMapping(path = "/api/v1/repos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Repository Scoring API", description = "Fetch and score GitHub repositories based on stars, forks, and recency")
public class RepoController {


    private final ScoringService scoringService;

    /**
     * Fetches repositories from GitHub and computes a repository score for each.
     *
     * @param language   Programming language to filter repositories by (required).
     * @param since      ISO 8601 date string; only repositories created after this date are included (required).
     * @param extraQuery Optional additional search query (e.g., topic:backend).
     * @param limit      Maximum number of repositories to return (default 100, minimum 1).
     * @return A {@link Mono} emitting a {@link RepoResponse} containing scored repositories
     * along with metadata such as total count and request details.
     */
    @GetMapping
    @Operation(
            summary = "Get scored repositories",
            description = "Fetch repositories from GitHub and compute a weighted score based on stars, forks, and recency."
    )
    public Mono<RepoResponse> getRepoScore(
            @Parameter(description = "Programming language filter", required = true)
            @RequestParam @NotBlank(message = "Language must not be blank")
            String language,

            @Parameter(description = "Fetch repositories created after this date (YYYY-MM-DD)", required = true)
            @RequestParam("since")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,

            @Parameter(description = "Optional GitHub topic filter, e.g., topic:backend")
            @RequestParam(value = "q", required = false)
            String extraQuery,

            @Parameter(description = "Maximum number of repositories to return")
            @RequestParam(value = "limit", defaultValue = "100")
            @Min(value = 1, message = "limit must be at least 1") int limit
    ) {
        return scoringService.scoreWithMetadata(language, since, extraQuery, limit);
    }
}
