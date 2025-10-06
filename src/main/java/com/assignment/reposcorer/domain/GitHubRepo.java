package com.assignment.reposcorer.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Represents a GitHub repository fetched from the GitHub API.
 * <p>
 * This record captures the main attributes needed for scoring and displaying repository data.
 * Unknown JSON properties are ignored to ensure forward compatibility with the GitHub API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepo(
        String name,

        @JsonProperty("full_name") String fullName,

        Owner owner,

        @JsonProperty("html_url") String htmlUrl,

        String language,

        @JsonProperty("stargazers_count") int stars,

        @JsonProperty("forks_count") int forks,

        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {

    /**
     * Represents the owner of the GitHub repository.
     * Only the login (username) is captured for scoring purposes.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(
            String login
    ) {
    }
}
