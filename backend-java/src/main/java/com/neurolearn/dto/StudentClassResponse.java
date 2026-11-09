package com.neurolearn.dto;

import java.time.OffsetDateTime;

/** A class as an enrolled student sees it. */
public record StudentClassResponse(Long id, String name, String teacherName, OffsetDateTime joinedAt) {
}
