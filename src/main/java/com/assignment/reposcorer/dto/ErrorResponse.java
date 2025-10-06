package com.assignment.reposcorer.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(OffsetDateTime date, int status, String message, String path) {}