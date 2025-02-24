package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Section;
import com.ai.hakaton.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findBySections(Section section);
}
