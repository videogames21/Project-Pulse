package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.user.dto.UserResponse;
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
    public Result findStudents(@RequestParam(required = false) String role,
                               @RequestParam(required = false) Boolean unassigned) {
        List<UserResponse> students = Boolean.TRUE.equals(unassigned)
                ? userService.findUnassignedStudents()
                : userService.findStudents();
        return new Result(true, StatusCode.SUCCESS, "Students retrieved successfully", students);
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Long id) {
        UserResponse user = userService.findById(id);
        return new Result(true, StatusCode.SUCCESS, "User retrieved successfully", user);
    }
}
