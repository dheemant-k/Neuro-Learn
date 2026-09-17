package com.neurolearn.controller;

import com.neurolearn.dto.QuestionResponse;
import com.neurolearn.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer difficulty
    ) {
        return ResponseEntity.ok(questionService.getQuestions(topic, difficulty));
    }

    @GetMapping("/random")
    public ResponseEntity<QuestionResponse> getRandomQuestion(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer difficulty
    ) {
        QuestionResponse q = questionService.getRandomQuestion(topic, difficulty);
        if (q == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(q);
    }

    @GetMapping("/topics")
    public ResponseEntity<List<String>> getTopics() {
        return ResponseEntity.ok(questionService.getAvailableTopics());
    }
}
