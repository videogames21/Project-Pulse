package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.user.dto.DeactivateRequest;
import edu.tcu.cs.projectpulse.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result findUsers(@RequestParam(required = false) String role,
                            @RequestParam(required = false) String name,
                            @RequestParam(required = false) Boolean unassigned,
                            @RequestParam(required = false) String status) {
        if ("INSTRUCTOR".equalsIgnoreCase(role)) {
            UserStatus statusFilter = null;
            if (status != null && !status.isBlank()) {
                try { statusFilter = UserStatus.valueOf(status.toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }
            List<UserResponse> instructors = userService.findInstructors(name, statusFilter);
            return new Result(true, StatusCode.SUCCESS, "Instructors retrieved successfully", instructors);
        }
        List<UserResponse> students = Boolean.TRUE.equals(unassigned)
                ? userService.findUnassignedStudents()
                : userService.findStudents();
        return new Result(true, StatusCode.SUCCESS, "Students retrieved successfully", students);
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        UserEntity entity = userService.findEntityById(id);
        if (entity.getRole() == UserRole.INSTRUCTOR) {
            return new Result(true, StatusCode.SUCCESS, "Instructor retrieved successfully",
                    userService.toInstructorDetail(entity));
        }
        return new Result(true, StatusCode.SUCCESS, "User retrieved successfully",
                userService.toResponse(entity));
    }

    @PatchMapping("/{id}/deactivate")
    public Result deactivateInstructor(@PathVariable Long id,
                                       @RequestBody @Valid DeactivateRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Instructor deactivated successfully",
                userService.deactivateInstructor(id, request.reason()));
    }

    @PatchMapping("/{id}/reactivate")
    public Result reactivateInstructor(@PathVariable Long id) {
        return new Result(true, StatusCode.SUCCESS, "Instructor reactivated successfully",
                userService.reactivateInstructor(id));
    }
}
