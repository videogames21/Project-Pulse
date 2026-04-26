package edu.tcu.cs.projectpulse.student;

import edu.tcu.cs.projectpulse.student.dto.DeleteStudentRequest;
import edu.tcu.cs.projectpulse.student.dto.StudentDetailResponse;
import edu.tcu.cs.projectpulse.student.dto.StudentSummaryResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public Result findStudents(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sectionName,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            Authentication authentication) {
        List<StudentSummaryResponse> results = studentService.findStudents(
                firstName, lastName, email, sectionName, sectionId,
                teamName, teamId, sortBy, sortDir, authentication);
        return new Result(true, StatusCode.SUCCESS, "Students retrieved successfully", results);
    }

    @GetMapping("/{id}")
    public Result viewStudent(@PathVariable Long id, Authentication authentication) {
        StudentDetailResponse detail = studentService.viewStudent(id, authentication);
        return new Result(true, StatusCode.SUCCESS, "Student retrieved successfully", detail);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id,
                              @Valid @RequestBody DeleteStudentRequest request,
                              Authentication authentication) {
        studentService.deleteStudent(id, request.adminPassword(), authentication);
    }
}
