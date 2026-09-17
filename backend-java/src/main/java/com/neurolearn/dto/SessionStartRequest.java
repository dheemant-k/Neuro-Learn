package com.neurolearn.dto;

import java.time.OffsetDateTime;

public class SessionStartRequest {
    private String topic;

    public SessionStartRequest() {}
    public SessionStartRequest(String topic) { this.topic = topic; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
