package com.ai.hakaton.controllers;

import com.ai.hakaton.models.Exam;
import com.ai.hakaton.services.ExamSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class ExamSchedulerController {
    private final ExamSchedulerService service;

    public ExamSchedulerController(ExamSchedulerService service) {
        this.service = service;
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<LocalDate, List<Exam>>> getSchedule(){
        return ResponseEntity.ok(service.scheduleExams());
    }
}
