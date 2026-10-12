package com.neurolearn.dto;

import java.util.List;

public class TeacherAnalyticsResponse {
    private Integer totalStudents;
    private Double classAvgXp;
    private Double classAvgAccuracy;
    private Double classAvgPKnow;
    private Integer totalQuestionsAttempted;
    private List<TopicBottleneck> topicBottlenecks;
    private List<StudentRosterItem> studentRoster;

    public TeacherAnalyticsResponse() {}

    public TeacherAnalyticsResponse(Integer totalStudents, Double classAvgXp, Double classAvgAccuracy,
                                    Double classAvgPKnow, Integer totalQuestionsAttempted,
                                    List<TopicBottleneck> topicBottlenecks, List<StudentRosterItem> studentRoster) {
        this.totalStudents = totalStudents;
        this.classAvgXp = classAvgXp;
        this.classAvgAccuracy = classAvgAccuracy;
        this.classAvgPKnow = classAvgPKnow;
        this.totalQuestionsAttempted = totalQuestionsAttempted;
        this.topicBottlenecks = topicBottlenecks;
        this.studentRoster = studentRoster;
    }

    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }

    public Double getClassAvgXp() { return classAvgXp; }
    public void setClassAvgXp(Double classAvgXp) { this.classAvgXp = classAvgXp; }

    public Double getClassAvgAccuracy() { return classAvgAccuracy; }
    public void setClassAvgAccuracy(Double classAvgAccuracy) { this.classAvgAccuracy = classAvgAccuracy; }

    public Double getClassAvgPKnow() { return classAvgPKnow; }
    public void setClassAvgPKnow(Double classAvgPKnow) { this.classAvgPKnow = classAvgPKnow; }

    public Integer getTotalQuestionsAttempted() { return totalQuestionsAttempted; }
    public void setTotalQuestionsAttempted(Integer totalQuestionsAttempted) { this.totalQuestionsAttempted = totalQuestionsAttempted; }

    public List<TopicBottleneck> getTopicBottlenecks() { return topicBottlenecks; }
    public void setTopicBottlenecks(List<TopicBottleneck> topicBottlenecks) { this.topicBottlenecks = topicBottlenecks; }

    public List<StudentRosterItem> getStudentRoster() { return studentRoster; }
    public void setStudentRoster(List<StudentRosterItem> studentRoster) { this.studentRoster = studentRoster; }

    public static class TopicBottleneck {
        private String topic;
        private Double avgPKnow;
        private Double avgAccuracy;
        private Integer studentsMastered;
        private Integer studentsStruggling;
        private Boolean bottleneckAlert;
        private String recommendation;

        public TopicBottleneck() {}

        public TopicBottleneck(String topic, Double avgPKnow, Double avgAccuracy,
                               Integer studentsMastered, Integer studentsStruggling,
                               Boolean bottleneckAlert, String recommendation) {
            this.topic = topic;
            this.avgPKnow = avgPKnow;
            this.avgAccuracy = avgAccuracy;
            this.studentsMastered = studentsMastered;
            this.studentsStruggling = studentsStruggling;
            this.bottleneckAlert = bottleneckAlert;
            this.recommendation = recommendation;
        }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public Double getAvgPKnow() { return avgPKnow; }
        public void setAvgPKnow(Double avgPKnow) { this.avgPKnow = avgPKnow; }

        public Double getAvgAccuracy() { return avgAccuracy; }
        public void setAvgAccuracy(Double avgAccuracy) { this.avgAccuracy = avgAccuracy; }

        public Integer getStudentsMastered() { return studentsMastered; }
        public void setStudentsMastered(Integer studentsMastered) { this.studentsMastered = studentsMastered; }

        public Integer getStudentsStruggling() { return studentsStruggling; }
        public void setStudentsStruggling(Integer studentsStruggling) { this.studentsStruggling = studentsStruggling; }

        public Boolean getBottleneckAlert() { return bottleneckAlert; }
        public void setBottleneckAlert(Boolean bottleneckAlert) { this.bottleneckAlert = bottleneckAlert; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    public static class StudentRosterItem {
        private Long studentProfileId;
        private Long userId;
        private String name;
        private String email;
        private Integer level;
        private Integer totalXp;
        private Double accuracy;
        private Double pKnow;
        private Integer questionsAttempted;
        private String status;

        public StudentRosterItem() {}

        public StudentRosterItem(Long studentProfileId, Long userId, String name, String email,
                                 Integer level, Integer totalXp, Double accuracy, Double pKnow,
                                 Integer questionsAttempted, String status) {
            this.studentProfileId = studentProfileId;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.level = level;
            this.totalXp = totalXp;
            this.accuracy = accuracy;
            this.pKnow = pKnow;
            this.questionsAttempted = questionsAttempted;
            this.status = status;
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

        public Integer getQuestionsAttempted() { return questionsAttempted; }
        public void setQuestionsAttempted(Integer questionsAttempted) { this.questionsAttempted = questionsAttempted; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
