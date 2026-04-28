package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import edu.tcu.cs.projectpulse.user.dto.AssignSectionRequest;
import edu.tcu.cs.projectpulse.user.dto.ChangePasswordRequest;
import edu.tcu.cs.projectpulse.user.dto.DeactivateRequest;
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
                            @RequestParam(required = false) Boolean unassigned,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Long sectionId) {
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
                ? userService.findUnassignedStudents(sectionId)
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

    @PatchMapping("/{id}/section")
    public Result assignSection(@PathVariable Long id, @RequestBody AssignSectionRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Instructor section updated successfully",
                userService.assignSection(id, request.sectionId()));
    }
}
