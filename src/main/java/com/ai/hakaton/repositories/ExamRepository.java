package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT DISTINCT e FROM Exam e " +
            "JOIN FETCH e.section s " +
            "JOIN FETCH s.students")
    List<Exam> findAllWithStudents();

    default List<Exam> findAllWithStudentsLogged() {
        System.out.println("Загрузка всех экзаменов с секциями и студентами");
        return findAllWithStudents();
    }
}