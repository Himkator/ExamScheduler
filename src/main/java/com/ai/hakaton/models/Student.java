package com.ai.hakaton.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@Data
public class Student {
    @Id
    private String id; // fake_id

    private String name;
    private String eduProgram;
    private String yearOfStudy;
    private int course;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "student_section",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    private Set<Section> sections = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "student_exam",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "exam_id")
    )
    @JsonIgnore
    private Set<Exam> exams = new HashSet<>();

    public Student(String id, String name, String eduProgram, String yearOfStudy, int course, Set<Section> sections, Set<Exam> exams) {
        this.id = id;
        this.name = name;
        this.eduProgram = eduProgram;
        this.yearOfStudy = yearOfStudy;
        this.course = course;
        this.sections = sections;
        this.exams = exams;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
        this.course = course;
    }

    public Set<Exam> getExams() {
        return exams;
    }

    public void setExams(Set<Exam> exams) {
        this.exams = exams;
    }

    public Student() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEduProgram() {
        return eduProgram;
    }

    public void setEduProgram(String eduProgram) {
        this.eduProgram = eduProgram;
    }

    public String getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(String yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public Set<Section> getSections() {
        return sections;
    }

    public void setSections(Set<Section> sections) {
        this.sections = sections;
    }
}
