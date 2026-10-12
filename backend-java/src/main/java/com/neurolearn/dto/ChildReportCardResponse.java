package com.neurolearn.dto;

import java.util.List;

public class ChildReportCardResponse {
    private Long studentProfileId;
    private Long userId;
    private String name;
    private String email;
    private DashboardResponse dashboard;
    private List<String> strengths;
    private List<String> areasForGrowth;
    private List<String> parentingTips;

    public ChildReportCardResponse() {}

    public ChildReportCardResponse(Long studentProfileId, Long userId, String name, String email,
                                   DashboardResponse dashboard, List<String> strengths,
                                   List<String> areasForGrowth, List<String> parentingTips) {
        this.studentProfileId = studentProfileId;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.dashboard = dashboard;
        this.strengths = strengths;
        this.areasForGrowth = areasForGrowth;
        this.parentingTips = parentingTips;
    }

    public Long getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public DashboardResponse getDashboard() { return dashboard; }
    public void setDashboard(DashboardResponse dashboard) { this.dashboard = dashboard; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getAreasForGrowth() { return areasForGrowth; }
    public void setAreasForGrowth(List<String> areasForGrowth) { this.areasForGrowth = areasForGrowth; }

    public List<String> getParentingTips() { return parentingTips; }
    public void setParentingTips(List<String> parentingTips) { this.parentingTips = parentingTips; }
}
