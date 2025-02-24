package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findByCode(String code);
}
