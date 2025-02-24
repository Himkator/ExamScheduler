package com.ai.hakaton.services;

import com.ai.hakaton.models.Auditorium;
import com.ai.hakaton.models.Exam;
import com.ai.hakaton.models.Section;
import com.ai.hakaton.models.Student;
import com.ai.hakaton.repositories.AuditoriumRepository;
import com.ai.hakaton.repositories.ExamRepository;
import com.ai.hakaton.repositories.SectionRepository;
import com.ai.hakaton.repositories.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class DataLoadService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private SectionRepository sectionRepository;

    // Метод для безопасного получения строкового значения из ячейки
    public static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((int) d);
                } else {
                    return String.valueOf(d);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    // Загрузка экзаменов, студентов и секций из Excel
    public String loadExamsAndStudents(String filePath) throws IOException {
        System.out.println("Загрузка данных экзаменов и студентов из: " + filePath);
        List<Exam> exams = new ArrayList<>();
        Map<String, Student> studentMap = new HashMap<>();
        // Для секций используем Map, чтобы не создавать дубликаты.
        Map<String, Section> sectionMap = new HashMap<>();

        FileInputStream fis = new FileInputStream(new File(filePath));
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        // Предполагается, что первая строка – заголовки:
        // Заголовки: Subject, Instructor, Course, EduProgram, YearsOfStudy, fake_name, fake_id, Section
        for (int i = 1; i <= 500; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            try {
                String subject = getCellStringValue(row.getCell(1));
                String instructor = getCellStringValue(row.getCell(2));
                String course = getCellStringValue(row.getCell(3));
                String eduProgram = getCellStringValue(row.getCell(4));
                String yearsOfStudyStr = getCellStringValue(row.getCell(5));
                String fakeName = getCellStringValue(row.getCell(7));
                String fakeIdStr = getCellStringValue(row.getCell(8));
                String sectionCode = getCellStringValue(row.getCell(9));

                // Значения по умолчанию для экзамена
                LocalDate examDate = LocalDate.parse("2025-02-24");
                LocalTime examTime = LocalTime.parse("09:00");
                int duration = 180;

                // Обработка секции
                Section section = sectionMap.get(sectionCode);
                if (section == null) {
                    Optional<Section> optSection = sectionRepository.findByCode(sectionCode);
                    if (optSection.isPresent()) {
                        section = optSection.get();
                    } else {
                        section = new Section();
                        section.setCode(sectionCode);
                        section = sectionRepository.save(section);
                    }
                    sectionMap.put(sectionCode, section);
                }

                // Создаем экзамен и связываем его с секцией
                Exam exam = new Exam(null, subject, instructor, course, null, examDate, examTime, duration, null);
                // Здесь устанавливаем связь экзамена с секцией
                exam.setSection(section);
                exams.add(exam);

                // Обработка студента (по fake_id)
                Student student = studentMap.get(fakeIdStr);
                if (student == null) {
                    Student optStudent = studentRepository.findById(fakeIdStr).orElse(null);
                    if (optStudent==null) {
                        student = new Student();
                        student.setId(fakeIdStr);
                        student.setName(fakeName);
                        student.setEduProgram(eduProgram);
                        student.setYearOfStudy(yearsOfStudyStr);
                        student.setSections(new HashSet<>());
                    } else {
                        student = optStudent;
                    }
                    studentMap.put(fakeIdStr, student);
                }
                // Добавляем секцию к студенту (many-to-many)
                student.getSections().add(section);
            } catch (Exception e) {
                System.out.println("Ошибка при обработке строки " + i + ": " + e.getMessage());
            }
        }
        workbook.close();
        fis.close();

        examRepository.saveAll(exams);
        studentRepository.saveAll(studentMap.values());
        System.out.println("Экзаменов загружено: " + exams.size());
        System.out.println("Студентов загружено: " + studentMap.size());
        System.out.println("Секций загружено: " + sectionMap.size());

        return "All data processed";
    }

    // Загрузка аудиторий из Excel
    public String loadAuditoriums(String filePath) throws IOException {
        System.out.println("Загрузка данных аудиторий из: " + filePath);
        List<Auditorium> auditoriums = new ArrayList<>();
        FileInputStream fis = new FileInputStream(new File(filePath));
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        // Предполагается, что первая строка – заголовки: RoomId, RoomType, Capacity
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String roomId = getCellStringValue(row.getCell(0));
            String roomType = getCellStringValue(row.getCell(1));
            int capacity = Integer.parseInt(getCellStringValue(row.getCell(2)));
            Auditorium auditorium = new Auditorium(null, roomId, roomType, capacity);
            auditoriums.add(auditorium);
        }
        workbook.close();
        fis.close();
        auditoriumRepository.saveAll(auditoriums);
        System.out.println("Аудиторий загружено: " + auditoriums.size());
        return "All data processed";
    }

    public String loadAuditoriums(MultipartFile file) throws IOException {
        System.out.println("Загрузка данных аудиторий из: " + file);
        List<Auditorium> auditoriums = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        // Предполагается, что первая строка – заголовки: RoomId, RoomType, Capacity
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String roomId = getCellStringValue(row.getCell(0));
            String roomType = getCellStringValue(row.getCell(1));
            int capacity = Integer.parseInt(getCellStringValue(row.getCell(2)));
            Auditorium auditorium = new Auditorium(null, roomId, roomType, capacity);
            auditoriums.add(auditorium);
        }
        workbook.close();
        auditoriumRepository.saveAll(auditoriums);
        System.out.println("Аудиторий загружено: " + auditoriums.size());
        return "All data processed";
    }

    public String loadExamsAndStudents(MultipartFile file) throws IOException {
        System.out.println("Загрузка данных экзаменов и студентов из: " + file);
        List<Exam> exams = new ArrayList<>();
        Map<String, Student> studentMap = new HashMap<>();
        // Для секций используем Map, чтобы не создавать дубликаты.
        Map<String, Section> sectionMap = new HashMap<>();

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        // Предполагается, что первая строка – заголовки:
        // Заголовки: Subject, Instructor, Course, EduProgram, YearsOfStudy, fake_name, fake_id, Section
        for (int i = 1; i <= 500; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            try {
                String subject = getCellStringValue(row.getCell(1));
                String instructor = getCellStringValue(row.getCell(2));
                String course = getCellStringValue(row.getCell(3));
                String eduProgram = getCellStringValue(row.getCell(4));
                String yearsOfStudyStr = getCellStringValue(row.getCell(5));
                String fakeName = getCellStringValue(row.getCell(7));
                String fakeIdStr = getCellStringValue(row.getCell(8));
                String sectionCode = getCellStringValue(row.getCell(9));

                // Значения по умолчанию для экзамена
                LocalDate examDate = LocalDate.parse("2025-02-24");
                LocalTime examTime = LocalTime.parse("09:00");
                int duration = 180;

                // Обработка секции
                Section section = sectionMap.get(sectionCode);
                if (section == null) {
                    Optional<Section> optSection = sectionRepository.findByCode(sectionCode);
                    if (optSection.isPresent()) {
                        section = optSection.get();
                    } else {
                        section = new Section();
                        section.setCode(sectionCode);
                        section = sectionRepository.save(section);
                    }
                    sectionMap.put(sectionCode, section);
                }

                // Создаем экзамен и связываем его с секцией
                Exam exam = new Exam(null, subject, instructor, course, null, examDate, examTime, duration, null);
                // Здесь устанавливаем связь экзамена с секцией
                exam.setSection(section);
                exams.add(exam);

                // Обработка студента (по fake_id)
                Student student = studentMap.get(fakeIdStr);
                if (student == null) {
                    Student optStudent = studentRepository.findById(fakeIdStr).orElse(null);
                    if (optStudent==null) {
                        student = new Student();
                        student.setId(fakeIdStr);
                        student.setName(fakeName);
                        student.setEduProgram(eduProgram);
                        student.setYearOfStudy(yearsOfStudyStr);
                        student.setSections(new HashSet<>());
                    } else {
                        student = optStudent;
                    }
                    studentMap.put(fakeIdStr, student);
                }
                // Добавляем секцию к студенту (many-to-many)
                student.getSections().add(section);
            } catch (Exception e) {
                System.out.println("Ошибка при обработке строки " + i + ": " + e.getMessage());
            }
        }
        workbook.close();

        examRepository.saveAll(exams);
        studentRepository.saveAll(studentMap.values());
        System.out.println("Экзаменов загружено: " + exams.size());
        System.out.println("Студентов загружено: " + studentMap.size());
        System.out.println("Секций загружено: " + sectionMap.size());

        return "All data processed";
    }
}
