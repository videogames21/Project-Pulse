package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.user.dto.ChangePasswordRequest;
import edu.tcu.cs.projectpulse.user.dto.UpdateProfileRequest;
import edu.tcu.cs.projectpulse.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result me(Authentication authentication) {
        UserResponse user = userService.findByEmail(authentication.getName());
        return new Result(true, StatusCode.SUCCESS, "Current user retrieved successfully", user);
    }

    @GetMapping("/me/profile")
    public Result getProfile(Authentication authentication) {
        return new Result(true, StatusCode.SUCCESS, "Profile retrieved successfully",
                userService.getProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public Result updateProfile(Authentication authentication,
                                @Valid @RequestBody UpdateProfileRequest req) {
        return new Result(true, StatusCode.SUCCESS, "Profile updated successfully",
                userService.updateProfile(authentication.getName(), req));
    }

    @PutMapping("/me/password")
    public Result changePassword(Authentication authentication,
                                 @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(authentication.getName(), req);
        return new Result(true, StatusCode.SUCCESS, "Password changed successfully", null);
    }

    @GetMapping
    public Result findUsers(@RequestParam(required = false) String role,
                            @RequestParam(required = false) String name,
                            @RequestParam(required = false) Boolean unassigned) {
        if ("INSTRUCTOR".equalsIgnoreCase(role)) {
            List<UserResponse> instructors = userService.findInstructors(name);
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
}
