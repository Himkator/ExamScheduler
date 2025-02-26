package com.ai.hakaton.services;

import com.ai.hakaton.models.Auditorium;
import com.ai.hakaton.models.Exam;
import com.ai.hakaton.models.Student;
import com.ai.hakaton.repositories.ExamRepository;
import com.ai.hakaton.repositories.StudentRepository;
import com.ai.hakaton.repositories.AuditoriumRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExamSchedulerService {
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(19, 30);
    private static final int EXAM_DURATION_MINUTES = 180; // 3 часа
    private static final int SCHEDULE_DAYS = 30;

    // Кэши для оптимизации доступа к данным
    private Map<Exam, List<Student>> studentsByExamCache = new HashMap<>();
    private Map<Exam, Integer> studentCountCache = new HashMap<>();

    @Transactional
    public Map<LocalDate, List<Exam>> scheduleExams() {
        System.out.println("Начинаем планирование экзаменов...");

        // Загружаем экзамены и аудитории
        List<Exam> exams = examRepository.findAllWithStudents();
        List<Auditorium> auditoriums = auditoriumRepository.findAll();

        System.out.println("Загружено " + exams.size() + " экзаменов и " + auditoriums.size() + " аудиторий");

        // Заполняем кэш студентов для экзаменов
        for (Exam exam : exams) {
            List<Student> students = studentRepository.findBySections(exam.getSection());
            studentsByExamCache.put(exam, students);
            studentCountCache.put(exam, students.size());
        }

        // Сортируем экзамены по количеству студентов (сначала большие группы)
        exams.sort(Comparator.comparingInt(studentCountCache::get).reversed());

        Map<LocalDate, List<Exam>> schedule = new TreeMap<>();
        Map<String, LocalDate> lastStudentExamDate = new HashMap<>();
        Map<String, LocalDate> lastInstructorExamDate = new HashMap<>();
        Map<LocalDate, Set<String>> scheduledSections = new HashMap<>();
        LocalDate startDate = LocalDate.now();

        for (Exam exam : exams) {
            boolean scheduled = false;

            for (int i = 0; i < SCHEDULE_DAYS && !scheduled; i++) {
                LocalDate date = startDate.plusDays(i);

                if (canScheduleExam(exam, date, lastStudentExamDate, lastInstructorExamDate, scheduledSections, schedule)) {
                    System.out.println("Назначаем экзамен " + exam.getSection() + " на " + date);

                    schedule.computeIfAbsent(date, k -> new ArrayList<>()).add(exam);
                    exam.setExamDate(date);
                    exam.setExamTime(findAvailableTime(schedule.get(date)));
                    exam.setAuditorium(findAvailableAuditorium(auditoriums, schedule.get(date), exam));

                    updateLastExamDates(exam, lastStudentExamDate, lastInstructorExamDate, scheduledSections, date);
                    scheduled = true;
                }
            }
            if (!scheduled) {
                System.out.println("Не удалось назначить экзамен " + exam.getSection());
            }
        }

        System.out.println("Планирование завершено. Сохраняем данные...");
        examRepository.saveAll(exams);
        return schedule;
    }

    private boolean canScheduleExam(Exam exam, LocalDate date,
                                    Map<String, LocalDate> lastStudentExamDate,
                                    Map<String, LocalDate> lastInstructorExamDate,
                                    Map<LocalDate, Set<String>> scheduledSections,
                                    Map<LocalDate, List<Exam>> schedule) {
        //Ограничение по количеству экзаменов в день
        if (schedule.getOrDefault(date, new ArrayList<>()).size() >= 20) {
            return false;
        }
        //Ограничение по пересечению студентов
        for (Student student : studentsByExamCache.get(exam)) {
            LocalDate lastExam = lastStudentExamDate.get(student.getName());
            if (lastExam != null && lastExam.plusDays(1).isAfter(date)) {
                return false;
            }
        }
//        LocalDate lastInstructorExam = lastInstructorExamDate.get(exam.getInstructor());
//        if (lastInstructorExam != null && lastInstructorExam.plusDays(1).isAfter(date)) {
//            return false;
//        }
        // Один проктор, одна аудитория
        LocalDate lastInstructorExam = lastInstructorExamDate.get(exam.getInstructor());
        if (lastInstructorExam != null && lastInstructorExam.isEqual(date)) {
            // Проверяем, есть ли уже экзамен с этим преподавателем в это же время
            List<Exam> examsOnThisDay = schedule.getOrDefault(date, new ArrayList<>());
            for (Exam e : examsOnThisDay) {
                if (e.getInstructor().equals(exam.getInstructor())) {
                    // Проверяем пересечение по времени
                    LocalTime existingExamEndTime = e.getExamTime().plusMinutes(EXAM_DURATION_MINUTES);
                    if (exam.getExamTime().isBefore(existingExamEndTime) &&
                            exam.getExamTime().plusMinutes(EXAM_DURATION_MINUTES).isAfter(e.getExamTime()) ||
                            exam.getExamTime().equals(existingExamEndTime)) {
                        return false; // Экзамены пересекаются или стыкуются без перерыва
                    }
                }
            }
        }
        //Ограничение на дублирование предметов в один день:
        Set<String> sectionsOnDate = scheduledSections.getOrDefault(date, new HashSet<>());
        if (sectionsOnDate.contains(exam.getSection())) {
            return false;
        }

        return true;
    }

    private Auditorium findAvailableAuditorium(List<Auditorium> auditoriums, List<Exam> exams, Exam exam) {
        int studentCount = studentCountCache.get(exam);

        for (Auditorium auditorium : auditoriums) {
            boolean occupied = exams.stream()
                    .anyMatch(e -> e.getAuditorium() != null && e.getAuditorium().equals(auditorium));
            //Ограничение по вместимости аудитории
            if (!occupied && auditorium.getCapacity() >= studentCount) {
                System.out.println("Экзамен " + exam.getSection() + " назначен в аудитории " + auditorium.getNumber());
                return auditorium;
            }
        }

        throw new RuntimeException("Нет доступных аудиторий с достаточной вместимостью для экзамена " + exam.getSection());
    }

    private LocalTime findAvailableTime(List<Exam> exams) {
        LocalTime currentTime = START_TIME;
        exams.sort(Comparator.comparing(Exam::getExamTime));

        for (Exam exam : exams) {
            if (exam.getExamTime() == null) continue;
            LocalTime examEndTime = exam.getExamTime().plusMinutes(EXAM_DURATION_MINUTES);
            //Ограничение по времени работы
            if (currentTime.plusMinutes(EXAM_DURATION_MINUTES).isBefore(exam.getExamTime())) {
                System.out.println("Назначено время: " + currentTime);
                return currentTime;
            }
            currentTime = examEndTime;
        }

        if (currentTime.plusMinutes(EXAM_DURATION_MINUTES).isAfter(END_TIME)) {
            System.out.println("Превышено время работы, перенос на утро.");
            return START_TIME;
        }
        System.out.println("Назначено время: " + currentTime);
        return currentTime;
    }

    private void updateLastExamDates(Exam exam, Map<String, LocalDate> lastStudentExamDate,
                                     Map<String, LocalDate> lastInstructorExamDate,
                                     Map<LocalDate, Set<String>> scheduledSections,
                                     LocalDate date) {
        for (Student student : studentsByExamCache.get(exam)) {
            lastStudentExamDate.put(student.getName(), date);
        }
        lastInstructorExamDate.put(exam.getInstructor(), date);
        scheduledSections.computeIfAbsent(date, k -> new HashSet<>()).add(exam.getSection().getCode());
    }

    public void exportScheduleToExcel(Map<LocalDate, List<Exam>> schedule) {
        System.out.println("Создание Excel-файла с расписанием экзаменов...");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Расписание экзаменов");

        // Форматирование заголовков
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        // Создание заголовка
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Дата", "Время", "Аудитория", "Экзамен", "Инструктор", "Студенты"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Форматтер для дат и времени
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        int rowNum = 1;
        for (Map.Entry<LocalDate, List<Exam>> entry : schedule.entrySet()) {
            LocalDate date = entry.getKey();
            for (Exam exam : entry.getValue()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(date.format(dateFormatter)); // Дата
                row.createCell(1).setCellValue(exam.getExamTime().format(timeFormatter)); // Время
                row.createCell(2).setCellValue(exam.getAuditorium().getNumber()); // Аудитория
                row.createCell(3).setCellValue(exam.getSection().getCode()); // Название экзамена
                row.createCell(4).setCellValue(exam.getInstructor()); // Инструктор

                // Студенты через запятую
                List<Student> students = studentsByExamCache.get(exam);
                String studentNames = students.stream().map(Student::getName).reduce((a, b) -> a + ", " + b).orElse("");
                row.createCell(5).setCellValue(studentNames);
            }
        }

        // Авторазмер колонок
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Сохранение файла
        try (FileOutputStream fileOut = new FileOutputStream("exam_schedule.xlsx")) {
            workbook.write(fileOut);
            System.out.println("Файл exam_schedule.xlsx успешно создан!");
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении файла: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                System.out.println("Ошибка при закрытии файла: " + e.getMessage());
            }
        }
    }
}
