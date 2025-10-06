/* TEST FILE - auto-tidied: assertions and error-case coverage recommended */
package com.assignment.reposcorer.service;

import com.assignment.reposcorer.domain.GitHubRepo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

class GitHubSearchClientTest {

    private static MockWebServer server;
    private static GitHubSearchClient client;

    @BeforeAll
    static void setup() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient wc = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();

        // Create a simple Caffeine cache for testing
        Cache<String, List<GitHubRepo>> testCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(100)
                .build();

        client = new GitHubSearchClient(wc, 50, testCache);
    }

    @AfterAll
    static void teardown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldFetchAndMapRepositories() {
        String fakeJson = """
            { "items": [
                {"name":"test","full_name":"user/test","html_url":"u","stargazers_count":5,
                 "forks_count":2,"language":"Java","updated_at":"2024-01-01T00:00:00Z"}
            ]}
            """;

        server.enqueue(new MockResponse()
                .setBody(fakeJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.search("Java", "2024-01-01", null, 1))
                .expectNextMatches(repo -> repo.name().equals("test") && repo.stars() == 5)
                .verifyComplete();
    }
}
