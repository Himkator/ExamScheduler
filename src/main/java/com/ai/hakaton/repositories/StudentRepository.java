package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Exam;
import com.ai.hakaton.models.Student;
import com.ai.hakaton.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    @Query("SELECT s FROM Student s " +
            "JOIN s.sections sec " +
            "WHERE sec = :section")
    List<Student> findBySections(Section section);

    default List<Student> findBySectionLogged(Section section) {
        System.out.println("Загрузка студентов для секции " + section.getId());
        return findBySections(section);
    }
    @Query("SELECT s FROM Student s " +
            "JOIN s.exams sec " +
            "WHERE sec = :exam")
    List<Student> findAllByExam(Exam exam);

}