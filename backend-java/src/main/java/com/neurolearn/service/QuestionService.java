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
        return getQuestions(topic, difficulty, null);
    }

    public List<QuestionResponse> getQuestions(String topic, Integer difficulty, String characterTheme) {
        List<Question> questions;
        if (characterTheme != null && !characterTheme.isBlank()) {
            if (topic != null && !topic.isBlank() && difficulty != null) {
                questions = questionRepository.findByTopicAndDifficultyAndCharacterTheme(topic, difficulty, characterTheme);
            } else if (topic != null && !topic.isBlank()) {
                questions = questionRepository.findByTopicAndCharacterTheme(topic, characterTheme);
            } else {
                questions = questionRepository.findAll().stream()
                        .filter(q -> characterTheme.equalsIgnoreCase(q.getCharacterTheme()))
                        .collect(Collectors.toList());
            }
            if (questions.isEmpty()) {
                return getQuestions(topic, difficulty, null);
            }
        } else {
            if (topic != null && !topic.isBlank() && difficulty != null) {
                questions = questionRepository.findByTopicAndDifficulty(topic, difficulty);
            } else if (topic != null && !topic.isBlank()) {
                questions = questionRepository.findByTopic(topic);
            } else {
                questions = questionRepository.findAll();
            }
        }
        return questions.stream().map(QuestionResponse::fromEntity).collect(Collectors.toList());
    }

    public QuestionResponse getRandomQuestion(String topic, Integer difficulty) {
        return getRandomQuestion(topic, difficulty, null);
    }

    public QuestionResponse getRandomQuestion(String topic, Integer difficulty, String characterTheme) {
        Optional<Question> questionOpt;
        if (characterTheme != null && !characterTheme.isBlank()) {
            if (topic != null && !topic.isBlank() && difficulty != null) {
                questionOpt = questionRepository.findRandomQuestionByTopicAndDifficultyAndCharacterTheme(topic, difficulty, characterTheme);
            } else if (topic != null && !topic.isBlank()) {
                questionOpt = questionRepository.findRandomQuestionByTopicAndCharacterTheme(topic, characterTheme);
            } else {
                questionOpt = questionRepository.findAll().stream()
                        .filter(q -> characterTheme.equalsIgnoreCase(q.getCharacterTheme()))
                        .findAny();
            }
            if (questionOpt.isEmpty()) {
                return getRandomQuestion(topic, difficulty, null);
            }
        } else {
            if (topic != null && !topic.isBlank() && difficulty != null) {
                questionOpt = questionRepository.findRandomQuestionByTopicAndDifficulty(topic, difficulty);
            } else if (topic != null && !topic.isBlank()) {
                questionOpt = questionRepository.findRandomQuestionByTopic(topic);
            } else {
                questionOpt = questionRepository.findAll().stream().findAny();
            }
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
