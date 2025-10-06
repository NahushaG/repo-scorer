package com.assignment.reposcorer.service;

import com.assignment.reposcorer.domain.GitHubRepo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Defines the contract for searching GitHub repositories.
 * <p>
 * Implementations can fetch repositories from GitHub API or any other source.
 */
@Component
public interface RepoSearchClient {

    /**
     * Searches for repositories based on language, creation date, and optional query.
     * Supports reactive streaming with backpressure via {@link Flux}.
     *
     * @param language   programming language filter
     * @param since      ISO date string; fetch repositories created after this date
     * @param extraQuery additional search terms (optional)
     * @param limit      maximum number of repositories to fetch
     * @return Flux stream of {@link GitHubRepo} objects
     */
    Flux<GitHubRepo> search(String language, String since, String extraQuery, int limit);
}
