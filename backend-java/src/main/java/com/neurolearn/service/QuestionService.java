package com.neurolearn.service;

import com.neurolearn.dto.QuestionResponse;
import com.neurolearn.entity.Question;
import com.neurolearn.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<QuestionResponse> getQuestions(String topic, Integer difficulty) {
        List<Question> questions;
        if (topic != null && !topic.isBlank() && difficulty != null) {
            questions = questionRepository.findByTopicAndDifficulty(topic, difficulty);
        } else if (topic != null && !topic.isBlank()) {
            questions = questionRepository.findByTopic(topic);
        } else {
            questions = questionRepository.findAll();
        }
        return questions.stream().map(QuestionResponse::fromEntity).collect(Collectors.toList());
    }

    public QuestionResponse getRandomQuestion(String topic, Integer difficulty) {
        Optional<Question> questionOpt;
        if (topic != null && !topic.isBlank() && difficulty != null) {
            questionOpt = questionRepository.findRandomQuestionByTopicAndDifficulty(topic, difficulty);
        } else if (topic != null && !topic.isBlank()) {
            questionOpt = questionRepository.findRandomQuestionByTopic(topic);
        } else {
            questionOpt = questionRepository.findAll().stream().findAny();
        }

        return questionOpt.map(QuestionResponse::fromEntity)
                .orElseGet(() -> {
                    // Fallback to any question under the topic
                    if (topic != null && !topic.isBlank()) {
                        return questionRepository.findRandomQuestionByTopic(topic)
                                .map(QuestionResponse::fromEntity)
                                .orElse(null);
                    }
                    return null;
                });
    }

    public List<String> getAvailableTopics() {
        return questionRepository.findDistinctTopics();
    }
}
