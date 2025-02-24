package com.ai.hakaton.repositories;

import com.ai.hakaton.models.Auditorium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
}