package com.neurolearn.dto;

import com.neurolearn.entity.Question;

public class QuestionResponse {
    private Long id;
    private String topic;
    private Integer difficulty;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private String explanation;
    private String subject;
    private String characterTheme;

    public QuestionResponse() {
    }

    public QuestionResponse(Long id, String topic, Integer difficulty, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String explanation, String subject) {
        this(id, topic, difficulty, questionText, optionA, optionB, optionC, optionD, correctAnswer, explanation, subject, "batman");
    }

    public QuestionResponse(Long id, String topic, Integer difficulty, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String explanation, String subject, String characterTheme) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.subject = subject;
        this.characterTheme = characterTheme;
    }

    public static QuestionResponse fromEntity(Question q) {
        if (q == null) return null;
        return new QuestionResponse(
                q.getId(),
                q.getTopic(),
                q.getDifficulty(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD(),
                q.getCorrectAnswer(),
                q.getExplanation(),
                q.getSubject(),
                q.getCharacterTheme()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCharacterTheme() { return characterTheme; }
    public void setCharacterTheme(String characterTheme) { this.characterTheme = characterTheme; }
}
