package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
}
