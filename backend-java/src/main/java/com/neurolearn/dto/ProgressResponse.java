package com.neurolearn.dto;

import java.util.List;

public class ProgressResponse {

    private Integer totalXp;
    private Integer level;
    private Double overallAccuracy;
    private Double overallPKnow;
    private Boolean bossUnlocked;
    private List<SectionProgressDto> sections;

    public ProgressResponse() {}

    public ProgressResponse(Integer totalXp, Integer level, Double overallAccuracy, Double overallPKnow, Boolean bossUnlocked, List<SectionProgressDto> sections) {
        this.totalXp = totalXp;
        this.level = level;
        this.overallAccuracy = overallAccuracy;
        this.overallPKnow = overallPKnow;
        this.bossUnlocked = bossUnlocked;
        this.sections = sections;
    }

    public static class SectionProgressDto {
        private String sectionId;
        private Integer attempts;
        private Integer correctCount;
        private Double accuracy;
        private Double pKnow;
        private Boolean isMastered;
        private Boolean isCompleted;
        private Boolean isUnlocked;

        public SectionProgressDto() {}

        public SectionProgressDto(String sectionId, Integer attempts, Integer correctCount, Double accuracy, Double pKnow, Boolean isMastered, Boolean isCompleted, Boolean isUnlocked) {
            this.sectionId = sectionId;
            this.attempts = attempts;
            this.correctCount = correctCount;
            this.accuracy = accuracy;
            this.pKnow = pKnow;
            this.isMastered = isMastered;
            this.isCompleted = isCompleted;
            this.isUnlocked = isUnlocked;
        }

        public String getSectionId() { return sectionId; }
        public void setSectionId(String sectionId) { this.sectionId = sectionId; }

        public Integer getAttempts() { return attempts; }
        public void setAttempts(Integer attempts) { this.attempts = attempts; }

        public Integer getCorrectCount() { return correctCount; }
        public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }

        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

        public Double getPKnow() { return pKnow; }
        public void setPKnow(Double pKnow) { this.pKnow = pKnow; }

        public Boolean getIsMastered() { return isMastered; }
        public void setIsMastered(Boolean isMastered) { this.isMastered = isMastered; }

        public Boolean getIsCompleted() { return isCompleted; }
        public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

        public Boolean getIsUnlocked() { return isUnlocked; }
        public void setIsUnlocked(Boolean isUnlocked) { this.isUnlocked = isUnlocked; }
    }

    public Integer getTotalXp() { return totalXp; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Double getOverallAccuracy() { return overallAccuracy; }
    public void setOverallAccuracy(Double overallAccuracy) { this.overallAccuracy = overallAccuracy; }

    public Double getOverallPKnow() { return overallPKnow; }
    public void setOverallPKnow(Double overallPKnow) { this.overallPKnow = overallPKnow; }

    public Boolean getBossUnlocked() { return bossUnlocked; }
    public void setBossUnlocked(Boolean bossUnlocked) { this.bossUnlocked = bossUnlocked; }

    public List<SectionProgressDto> getSections() { return sections; }
    public void setSections(List<SectionProgressDto> sections) { this.sections = sections; }
}
