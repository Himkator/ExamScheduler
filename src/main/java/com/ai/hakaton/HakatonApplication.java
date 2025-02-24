package com.ai.hakaton;

import com.ai.hakaton.models.Exam;
import com.ai.hakaton.services.DataLoadService;
import com.ai.hakaton.services.ExamSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class HakatonApplication {
    @Autowired
    private DataLoadService dataLoadService;
    @Autowired
    private ExamSchedulerService examSchedulerService;

    public static void main(String[] args) {
        var context = SpringApplication.run(HakatonApplication.class, args);
//        DataLoadService service = context.getBean(DataLoadService.class);
//        try {
//            // Укажите корректные пути к файлам
//            service.loadAuditoriums("C:\\Users\\himka\\IdeaProjects\\Practice1\\src\\main\\java\\com\\ai\\auditoriums.xlsx");
//            service.loadExamsAndStudents("C:\\Users\\himka\\IdeaProjects\\Practice1\\src\\main\\java\\com\\ai\\FakedNarxozData.xlsx");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        ExamSchedulerService service=context.getBean(ExamSchedulerService.class);
        Map<LocalDate, List<Exam>> a=service.scheduleExams();
        for(LocalDate l:a.keySet()){
            System.out.println("-------");
            System.out.println(l+": ");
            for (Exam e:a.get(l)){
                System.out.println("-"+e.getSubject()+" "+e.getSection().getCode()+" "+e.getInstructor()
                +" "+e.getExamDate()+" "+e.getExamTime()+" "+e.getAuditorium().getNumber());
                System.out.println(e.getSection().getStudents().size()+": "+e.getAuditorium().getCapacity());
            }
            System.out.println("-------");
        }
    }
}
