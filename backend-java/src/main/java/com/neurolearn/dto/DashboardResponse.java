package com.neurolearn.dto;

import java.util.List;

public class DashboardResponse {
    private Integer total_xp;
    private Integer level;
    private Integer total_answered;
    private Integer total_correct;
    private Double overall_accuracy;
    private List<SectionStat> sections;
    private List<HistoryItem> recent_history;

    public DashboardResponse() {}

    public DashboardResponse(Integer total_xp, Integer level, Integer total_answered, Integer total_correct, Double overall_accuracy, List<SectionStat> sections, List<HistoryItem> recent_history) {
        this.total_xp = total_xp;
        this.level = level;
        this.total_answered = total_answered;
        this.total_correct = total_correct;
        this.overall_accuracy = overall_accuracy;
        this.sections = sections;
        this.recent_history = recent_history;
    }

    public static class SectionStat {
        private String section_id;
        private Integer answered;
        private Integer correct;
        private Double accuracy;
        private Integer best_streak;

        public SectionStat() {}

        public SectionStat(String section_id, Integer answered, Integer correct, Double accuracy, Integer best_streak) {
            this.section_id = section_id;
            this.answered = answered;
            this.correct = correct;
            this.accuracy = accuracy;
            this.best_streak = best_streak;
        }

        public String getSection_id() { return section_id; }
        public void setSection_id(String section_id) { this.section_id = section_id; }

        public Integer getAnswered() { return answered; }
        public void setAnswered(Integer answered) { this.answered = answered; }

        public Integer getCorrect() { return correct; }
        public void setCorrect(Integer correct) { this.correct = correct; }

        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

        public Integer getBest_streak() { return best_streak; }
        public void setBest_streak(Integer best_streak) { this.best_streak = best_streak; }
    }

    public static class HistoryItem {
        private String section_id;
        private Boolean correct;
        private Double rt;

        public HistoryItem() {}

        public HistoryItem(String section_id, Boolean correct, Double rt) {
            this.section_id = section_id;
            this.correct = correct;
            this.rt = rt;
        }

        public String getSection_id() { return section_id; }
        public void setSection_id(String section_id) { this.section_id = section_id; }

        public Boolean getCorrect() { return correct; }
        public void setCorrect(Boolean correct) { this.correct = correct; }

        public Double getRt() { return rt; }
        public void setRt(Double rt) { this.rt = rt; }
    }

    public Integer getTotal_xp() { return total_xp; }
    public void setTotal_xp(Integer total_xp) { this.total_xp = total_xp; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getTotal_answered() { return total_answered; }
    public void setTotal_answered(Integer total_answered) { this.total_answered = total_answered; }

    public Integer getTotal_correct() { return total_correct; }
    public void setTotal_correct(Integer total_correct) { this.total_correct = total_correct; }

    public Double getOverall_accuracy() { return overall_accuracy; }
    public void setOverall_accuracy(Double overall_accuracy) { this.overall_accuracy = overall_accuracy; }

    public List<SectionStat> getSections() { return sections; }
    public void setSections(List<SectionStat> sections) { this.sections = sections; }

    public List<HistoryItem> getRecent_history() { return recent_history; }
    public void setRecent_history(List<HistoryItem> recent_history) { this.recent_history = recent_history; }
}
