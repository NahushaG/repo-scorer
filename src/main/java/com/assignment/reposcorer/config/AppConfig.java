package com.assignment.reposcorer.config;

import com.assignment.reposcorer.domain.GitHubRepo;
import com.assignment.reposcorer.domain.ScoredRepo;
import com.assignment.reposcorer.dto.RepoResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Basic application configuration for WebClient and caching.
 */
@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /**
     * Creates a WebClient instance configured for the GitHub API.
     * Uses the {@code GITHUB_TOKEN} environment variable if available.
     */
    @Bean
    public WebClient githubClient(@Value("${github.baseUrl}") String baseUrl) {
        String token = System.getenv("GITHUB_TOKEN");

        var strategies = ExchangeStrategies.builder()
                //// Increase buffer size to handle large JSON payloads (e.g., multiple repository pages)
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        var builder = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .defaultHeader("Accept", "application/vnd.github+json");

        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token.trim());
            log.debug("GitHub client configured with token authentication");
        } else {
            log.warn("No GITHUB_TOKEN provided — using unauthenticated GitHub access (rate-limited)");
        }

        return builder.build();
    }

    @Bean
    public Cache<String, List<GitHubRepo>> repoCache(
            @Value("${repoScorer.cacheSeconds}") int cacheSeconds) {

        log.debug("Initializing repoCache with TTL {} seconds", cacheSeconds);

        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))
                .maximumSize(1000)
                .recordStats() // enables metrics for debugging
                .build();
    }

    @Bean
    public Cache<String, List<ScoredRepo>> scoreCache(
            @Value("${repoScorer.cacheSeconds}") int cacheSeconds) {

        log.debug("Initializing scoreCache with TTL {} seconds", cacheSeconds);

        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))  //Auto Eviction
                .maximumSize(1000) //max cache entries
                .recordStats() //enable metrics
                .build();
    }
}
