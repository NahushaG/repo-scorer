package com.assignment.reposcorer.dto;

import com.assignment.reposcorer.domain.ScoredRepo;

import java.util.List;

/**
 * Wrapper for GitHub repo response with metadata.
 */
public record RepoResponse(
        String language,
        String since,
        int limit,
        int count,
        int total,
        List<ScoredRepo> data
) {}
