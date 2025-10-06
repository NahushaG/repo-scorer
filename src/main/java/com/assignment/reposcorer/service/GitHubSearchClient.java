package com.assignment.reposcorer.service;

import com.assignment.reposcorer.domain.GitHubRepo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Client for searching GitHub repositories using the GitHub REST API.
 * <p>
 * Supports paginated fetching with optional language, creation date, and extra query filters.
 */
@Component
public class GitHubSearchClient implements RepoSearchClient {

    private final WebClient webClient;
    private final int perPage;
    private final Cache<String, List<GitHubRepo>> repoCache;
    private static final Logger log = LoggerFactory.getLogger(GitHubSearchClient.class);

    /**
     * Constructs the GitHubSearchClient.
     *
     * @param githubClient pre-configured WebClient bean
     * @param perPage      number of repositories per page to fetch
     */
    public GitHubSearchClient(WebClient githubClient,
                              @Value("${repoScorer.perPage}") int perPage,
                              Cache<String, List<GitHubRepo>> repoCache) {
        this.webClient = githubClient;
        this.perPage = perPage;
        this.repoCache = repoCache;
    }

    /**
     * Searches GitHub repositories according to language, creation date, and additional query.
     * Handles pagination automatically and caps at GitHub's 1000 results limit.
     *
     * @param language   programming language filter
     * @param since      ISO date string to filter repositories created after this date
     * @param extraQuery additional query terms (optional)
     * @param limit      maximum number of repositories to return
     * @return Flux of GitHubRepo objects
     */
    @Override

    public Flux<GitHubRepo> search(String language, String since, String extraQuery, int limit) {
        String cacheKey = String.format("%s|%s|%s|%d", language, since, extraQuery, limit);
        List<GitHubRepo> cached = repoCache.getIfPresent(cacheKey);

        if (cached != null && !cached.isEmpty()) {
            log.debug("Cache hit for query: {}", cacheKey);
            return Flux.fromIterable(cached);
        }

        log.debug("Cache miss for query: {} — fetching from GitHub API", cacheKey);

        String query = buildQuery(language, since, extraQuery);
        int pages = Math.min((limit + perPage - 1) / perPage, 10); // GitHub API caps at 1000 results

        return Flux.range(1, pages)
                .concatMap(page -> fetchPage(query, page))
                .take(limit)
                .collectList()
                .doOnNext(list -> repoCache.put(cacheKey, list))
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(e -> {
                    log.error("Error fetching repositories: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Fetches a single page of search results from GitHub.
     *
     * @param query search query string
     * @param page  page number to fetch
     * @return Flux of GitHubRepo objects
     */
    private Flux<GitHubRepo> fetchPage(String query, int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/repositories")
                        .queryParam("q", query)
                        .queryParam("sort", "stars")
                        .queryParam("order", "desc")
                        .queryParam("per_page", perPage)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .flatMapMany(resp -> Flux.fromIterable(resp.items));
    }

    /**
     * Builds the GitHub search query from language, creation date, and extra query.
     *
     * @param language   programming language filter
     * @param since      ISO date string for created-after filter
     * @param extraQuery optional extra query string
     * @return concatenated search query string
     */
    private String buildQuery(String language, String since, String extraQuery) {
        StringBuilder sb = new StringBuilder();
        if (language != null && !language.isBlank()) sb.append("language:").append(language).append(" ");
        if (since != null && !since.isBlank()) sb.append("created:>").append(since).append(" ");
        if (extraQuery != null && !extraQuery.isBlank()) sb.append(extraQuery);
        return sb.toString().trim();
    }

    /**
     * Internal DTO representing the GitHub search API response.
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SearchResponse {
        private List<GitHubRepo> items;
    }
}
