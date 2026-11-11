package com.neurolearn.repository;

import com.neurolearn.entity.QuestionMisconception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionMisconceptionRepository extends JpaRepository<QuestionMisconception, Long> {
    List<QuestionMisconception> findByQuestionIdIn(Collection<Long> questionIds);
    List<QuestionMisconception> findByQuestionId(Long questionId);
}
