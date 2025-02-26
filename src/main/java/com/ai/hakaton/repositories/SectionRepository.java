package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findByCode(String code);

    @Query("SELECT s FROM Section s " +
            "JOIN FETCH s.students " +
            "WHERE s.id = :id")
    Optional<Section> findByIdWithStudents(Long id);
}
