package com.assignment.reposcorer.domain;

import java.time.OffsetDateTime;

/**
 * Represents a GitHub repository along with a computed repository score.
 * <p>
 * The score is based on stars, forks, and recency of updates.
 */
public record ScoredRepo(
        String name,          // Repository name
        String fullName,      // Full repository name (owner/repo)
        String owner,         // Owner's username
        String url,           // URL of the repository
        String language,      // Primary programming language
        int stars,            // Number of stars
        int forks,            // Number of forks
        OffsetDateTime updatedAt, // Last updated timestamp
        double score          // Computed repo score
) {}
