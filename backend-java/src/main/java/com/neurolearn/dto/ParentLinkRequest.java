package com.neurolearn.dto;

public class ParentLinkRequest {
    private String studentEmail;

    public ParentLinkRequest() {}

    public ParentLinkRequest(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
}
