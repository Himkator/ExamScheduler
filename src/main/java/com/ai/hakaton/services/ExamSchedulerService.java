package com.ai.hakaton.services;

import com.ai.hakaton.models.Auditorium;
import com.ai.hakaton.models.Exam;
import com.ai.hakaton.models.Student;
import com.ai.hakaton.repositories.ExamRepository;
import com.ai.hakaton.repositories.StudentRepository;
import com.ai.hakaton.repositories.AuditoriumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class ExamSchedulerService {
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    private final LocalTime START_TIME = LocalTime.of(9, 0);
    private final LocalTime END_TIME = LocalTime.of(19, 30);
    private final int EXAM_DURATION = 3;
    private final int MAX_EXAMS_PER_DAY = 4;
    private final int SCHEDULE_DAYS = 30;

    public Map<LocalDate, List<Exam>> scheduleExams() {
        List<Exam> exams = examRepository.findAll();
        List<Auditorium> auditoriums = auditoriumRepository.findAll();
        exams.sort(Comparator.comparingInt(this::getStudentCount).reversed());

        Map<LocalDate, List<Exam>> schedule = new TreeMap<>();
        Map<String, LocalDate> lastStudentExamDate = new HashMap<>();
        Map<String, LocalDate> lastInstructorExamDate = new HashMap<>();
        LocalDate startDate = LocalDate.now();

        for (Exam exam : exams) {
            boolean scheduled = false;
            for (int i = 0; i < SCHEDULE_DAYS && !scheduled; i++) {
                LocalDate date = startDate.plusDays(i);
                if (canScheduleExam(exam, date, lastStudentExamDate, lastInstructorExamDate, schedule)) {
                    schedule.putIfAbsent(date, new ArrayList<>());
                    schedule.get(date).add(exam);

                    exam.setExamDate(date);
                    exam.setExamTime(findAvailableTime(schedule.get(date)));
                    exam.setAuditorium(findAvailableAuditorium(auditoriums, schedule.get(date), exam));

                    updateLastExamDates(exam, lastStudentExamDate, lastInstructorExamDate, date);
                    scheduled = true;
                }
            }
        }
        examRepository.saveAll(exams);
        return schedule;
    }

    private boolean canScheduleExam(Exam exam, LocalDate date, Map<String, LocalDate> lastStudentExamDate, Map<String, LocalDate> lastInstructorExamDate, Map<LocalDate, List<Exam>> schedule) {
        if (schedule.getOrDefault(date, new ArrayList<>()).size() >= MAX_EXAMS_PER_DAY) {
            return false;
        }
        for (Student student : getStudentsInExam(exam)) {
            LocalDate lastExam = lastStudentExamDate.get(student.getName());
            if (lastExam != null && lastExam.plusDays(1).isAfter(date)) {
                return false;
            }
        }
        LocalDate lastInstructorExam = lastInstructorExamDate.get(exam.getInstructor());
        return lastInstructorExam == null || lastInstructorExam.plusDays(1).isBefore(date);
    }

    private Auditorium findAvailableAuditorium(List<Auditorium> auditoriums, List<Exam> exams, Exam exam) {
        int studentCount = getStudentCount(exam); // Получаем количество студентов на экзамене

        for (Auditorium auditorium : auditoriums) {
            boolean occupied = exams.stream()
                    .anyMatch(e -> e.getAuditorium() != null && e.getAuditorium().equals(auditorium));

            // Проверяем, свободна ли аудитория и подходит ли по вместимости
            if (!occupied && auditorium.getCapacity() >= studentCount) {
                return auditorium;
            }
        }

        throw new RuntimeException("Нет доступных аудиторий с достаточной вместимостью");
    }



    private LocalTime findAvailableTime(List<Exam> exams) {
        LocalTime currentTime = START_TIME;
        exams.sort(Comparator.comparing(Exam::getExamTime)); // Сортируем экзамены по времени

        for (Exam exam : exams) {
            if (exam.getExamTime() == null) continue; // Пропускаем экзамены без времени
            LocalTime examEndTime = exam.getExamTime().plusHours(EXAM_DURATION);

            // Если между экзаменами есть свободное окно — ставим
            if (currentTime.plusHours(EXAM_DURATION).isBefore(exam.getExamTime())) {
                return currentTime;
            }
            currentTime = examEndTime;
        }

        // Если не нашли место в этом дне — вернуть стартовое время, чтобы пробовать следующий день
        if (currentTime.plusHours(EXAM_DURATION).isAfter(END_TIME)) {
            return START_TIME;
        }

        return currentTime;
    }



    private void updateLastExamDates(Exam exam, Map<String, LocalDate> lastStudentExamDate, Map<String, LocalDate> lastInstructorExamDate, LocalDate date) {
        for (Student student : getStudentsInExam(exam)) {
            lastStudentExamDate.put(student.getName(), date);
        }
        lastInstructorExamDate.put(exam.getInstructor(), date);
    }

    private List<Student> getStudentsInExam(Exam exam) {
        return studentRepository.findBySections(exam.getSection());
    }

    private int getStudentCount(Exam exam) {
        return getStudentsInExam(exam).size();
    }
}
