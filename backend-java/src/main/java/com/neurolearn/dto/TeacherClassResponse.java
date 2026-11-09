package com.neurolearn.dto;

import java.time.OffsetDateTime;

/** A class as its teacher sees it, including the join code to share with students. */
public record TeacherClassResponse(Long id, String name, String joinCode, long studentCount, OffsetDateTime createdAt) {
}
