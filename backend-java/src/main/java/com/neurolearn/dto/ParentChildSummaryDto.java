package com.neurolearn.dto;

import java.time.OffsetDateTime;

public class ParentChildSummaryDto {
    private Long studentProfileId;
    private Long userId;
    private String name;
    private String email;
    private Integer level;
    private Integer totalXp;
    private Double accuracy;
    private Double pKnow;
    private OffsetDateTime linkedAt;

    public ParentChildSummaryDto() {}

    public ParentChildSummaryDto(Long studentProfileId, Long userId, String name, String email,
                                 Integer level, Integer totalXp, Double accuracy, Double pKnow, OffsetDateTime linkedAt) {
        this.studentProfileId = studentProfileId;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.level = level;
        this.totalXp = totalXp;
        this.accuracy = accuracy;
        this.pKnow = pKnow;
        this.linkedAt = linkedAt;
    }

    public Long getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getTotalXp() { return totalXp; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

    public Double getPKnow() { return pKnow; }
    public void setPKnow(Double pKnow) { this.pKnow = pKnow; }

    public OffsetDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(OffsetDateTime linkedAt) { this.linkedAt = linkedAt; }
}
