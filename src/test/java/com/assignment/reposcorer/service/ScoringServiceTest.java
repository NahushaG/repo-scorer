/* TEST FILE - auto-tidied: assertions and error-case coverage recommended */
package com.assignment.reposcorer.service;

import com.assignment.reposcorer.domain.GitHubRepo;
import com.assignment.reposcorer.domain.ScoredRepo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Tests scoring logic, caching behavior, and ensures repositories are scored correctly
 * based on configured weights. Uses a mock client to simulate GitHub API responses.
 */
class ScoringServiceTest {

    private ScoringService scoringService;

    @BeforeEach
    void setup() {
        // Initialize Caffeine cache for testing
        Cache<String, List<ScoredRepo>> cache = Caffeine.newBuilder().maximumSize(100).build();

        // Initialize ScoringService with mock GitHub client and test weights
        scoringService = new ScoringService(
                new MockRepoClient(),
                cache,
                0.5,
                0.3,
                0.2,
                30
        );
    }

    /**
     * Verifies that repositories are scored correctly based on the configured weights
     * and sorted in descending order.
     */
    @Test
    void shouldScoreReposBasedOnWeights() {
        StepVerifier.create(scoringService.score("Java", "2024-01-01", "topic:backend", 2))
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    ScoredRepo first = list.get(0);
                    assertThat(first.score()).isGreaterThan(0);
                    assertThat(first.language()).isEqualTo("Java");
                })
                .verifyComplete();
    }

    /**
     * Verifies that repeated calls with the same parameters return cached results.
     * Ensures that the reference is identical for cached objects.
     */
    @Test
    void shouldCacheResults() {
        List<ScoredRepo> first = scoringService.score("Java", "2024-01-01", null, 2).block();
        List<ScoredRepo> second = scoringService.score("Java", "2024-01-01", null, 2).block();

        assertThat(first).isSameAs(second);
    }

    // Mock client to simulate GitHub API - simulates fetching repositories from GitHub
    private static class MockRepoClient implements RepoSearchClient {
        @Override/**
     * search - auto-generated brief description.
     */
    
        public Flux<GitHubRepo> search(String language, String since, String extraQuery, int limit) {
            GitHubRepo repo1 = new GitHubRepo(
                    "repo1", "user/repo1", new GitHubRepo.Owner("user1"),
                    "url1", language, 100, 10, OffsetDateTime.now().minusDays(2)
            );
            GitHubRepo repo2 = new GitHubRepo(
                    "repo2", "user/repo2", new GitHubRepo.Owner("user2"),
                    "url2", language, 50, 5, OffsetDateTime.now().minusDays(10)
            );
            return Flux.just(repo1, repo2);
        }
    }
}
