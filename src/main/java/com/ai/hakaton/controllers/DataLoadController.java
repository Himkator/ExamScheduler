package com.ai.hakaton.controllers;

import com.ai.hakaton.models.Exam;
import com.ai.hakaton.services.DataLoadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api")
public class DataLoadController {
    private final DataLoadService service;

    public DataLoadController(DataLoadService service) {
        this.service = service;
    }

    @PostMapping("/dataLoad/auditorium")
    public ResponseEntity<String> loadAuditorium(@RequestParam(value = "auditorium")
                                                     MultipartFile auditorium) throws IOException {
        return ResponseEntity.ok(service.loadAuditoriums(auditorium));
    }

    @PostMapping("/dataLoad/exams")
    public ResponseEntity<String> loadExams(@RequestParam(value = "exams")
                                                     MultipartFile exams) throws IOException {
        return ResponseEntity.ok(service.loadExamsAndStudents(exams));
    }
}
