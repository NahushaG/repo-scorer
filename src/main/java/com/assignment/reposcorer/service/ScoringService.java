package com.assignment.reposcorer.service;

import com.assignment.reposcorer.domain.GitHubRepo;
import com.assignment.reposcorer.domain.ScoredRepo;
import com.assignment.reposcorer.dto.RepoResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Service responsible for fetching and scoring GitHub repositories based on configurable weights.
 * <p>
 * Supports caching of results using Caffeine to improve performance for repeated queries.
 * Provides logging of cache metrics such as hits, misses, evictions, and estimated size for debugging.
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final RepoSearchClient client;
    private final Cache<String, List<ScoredRepo>> cache;

    private final double wStars;
    private final double wForks;
    private final double wRecency;
    private final double halfLifeDays;

    /**
     * Constructs the ScoringService with configurable weights and caching.
     *
     * @param client       the GitHub repository search client
     * @param cache        Caffeine cache for scored repositories
     * @param wStars       weight for repository stars
     * @param wForks       weight for repository forks
     * @param wRecency     weight for repository recency
     * @param halfLifeDays half-life in days used for decaying recency score
     */
    public ScoringService(@Qualifier("gitHubSearchClient") RepoSearchClient client,
                          Cache<String, List<ScoredRepo>> cache,
                          @Value("${repoScorer.weights.stars}") double wStars,
                          @Value("${repoScorer.weights.forks}") double wForks,
                          @Value("${repoScorer.weights.recency}") double wRecency,
                          @Value("${repoScorer.recencyHalfLifeDays}") double halfLifeDays) {

        double sum = wStars + wForks + wRecency;
        this.wStars = wStars / sum;
        this.wForks = wForks / sum;
        this.wRecency = wRecency / sum;
        this.halfLifeDays = halfLifeDays;

        this.client = client;
        this.cache = cache;
    }

    /**
     * Scores repositories and wraps them with additional metadata.
     *
     * @param language   the programming language to filter repositories
     * @param since      the creation date filter (repositories created after this date)
     * @param extraQuery optional extra GitHub query (e.g., "topic:backend")
     * @param limit      maximum number of repositories to return
     * @return a Mono emitting a {@link RepoResponse} containing scored repositories and metadata
     */

    public Mono<RepoResponse> scoreWithMetadata(String language, LocalDate since, String extraQuery, int limit) {
        OffsetDateTime sinceDateTime = since.atStartOfDay().atOffset(ZoneOffset.UTC);

        // Delegate to score method
        return score(language, sinceDateTime.toString(), extraQuery, limit)
                .map(list -> new RepoResponse(
                        language,
                        sinceDateTime.toString(),
                        limit,
                        list.size(),                   // actual count of returned repos
                        Math.min(list.size(), 1000),   // GitHub API max cap
                        list
                ));
    }

    /**
     * Scores repositories and returns a sorted list. Results are cached for performance.
     *
     * @param language   the programming language filter
     * @param since      ISO 8601 date string; fetch repositories created after this date
     * @param extraQuery optional extra GitHub query
     * @param limit      maximum number of repositories to return
     * @return Mono emitting a list of {@link ScoredRepo}, sorted descending by score
     */

    public Mono<List<ScoredRepo>> score(String language, String since, String extraQuery, int limit) {
        String cacheKey = String.join("|", language, since, extraQuery == null ? "" : extraQuery, String.valueOf(limit));
        /* Check cache first */
        List<ScoredRepo> cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            logCacheStats("Cache hit for key " + cacheKey);
            return Mono.just(cached);
        }

        // Fetch from client, score, sort, and cache results
        return client.search(language, since, extraQuery, limit)
                .filter(Objects::nonNull)
                .map(this::toScored)
                .sort((a, b) -> Double.compare(b.score(), a.score()))
                .collectList()
                .doOnNext(list -> {
                    cache.put(cacheKey, list);
                    logCacheStats("Cache updated for key " + cacheKey);
                });
    }

    /**
     * Converts a {@link GitHubRepo} to a {@link ScoredRepo} using configured weights.
     *
     * @param repo the GitHub repository
     * @return a scored repository
     */
    private ScoredRepo toScored(GitHubRepo repo) {
        double starsScore = Math.log1p(repo.stars());
        double forksScore = Math.log1p(repo.forks());
        double recencyScore = recencyScore(repo.updatedAt());

        double totalScore = wStars * starsScore + wForks * forksScore + wRecency * recencyScore;

        return new ScoredRepo(
                repo.name(),
                repo.fullName(),
                repo.owner() != null ? repo.owner().login() : "",
                repo.htmlUrl(),
                repo.language(),
                repo.stars(),
                repo.forks(),
                repo.updatedAt(),
                Math.round(totalScore * 1_000_000d) / 1_000_000d
        );
    }

    /**
     * Computes a decayed recency score based on how recently the repository was updated.
     * Uses an exponential decay based on the configured half-life.
     *
     * @param updatedAt the repository last updated timestamp
     * @return a score in range (0,1] reflecting recency
     */
    private double recencyScore(OffsetDateTime updatedAt) {
        if (updatedAt == null) return 0;

        long daysSinceUpdate = Math.max(0, Duration.between(updatedAt, OffsetDateTime.now()).toDays());
        double decayRate = Math.log(2.0) / halfLifeDays;

        return Math.exp(-decayRate * daysSinceUpdate);
    }

    /**
     * Logs cache metrics including hits, misses, evictions, and estimated size.
     *
     * @param prefix a message prefix for context
     */
    private void logCacheStats(String prefix) {
        CacheStats stats = cache.stats();
        log.debug("{} - hits: {}, misses: {}, evictions: {}, estimatedSize: {}",
                prefix, stats.hitCount(), stats.missCount(), stats.evictionCount(), cache.estimatedSize());
    }
}
