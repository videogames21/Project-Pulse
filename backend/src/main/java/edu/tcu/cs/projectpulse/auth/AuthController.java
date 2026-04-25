package edu.tcu.cs.projectpulse.auth;

import edu.tcu.cs.projectpulse.auth.dto.LoginRequest;
import edu.tcu.cs.projectpulse.auth.dto.RegisterRequest;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Account created successfully.",
                authService.register(request));
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest request) {
        return new Result(true, StatusCode.SUCCESS, "Login successful.",
                authService.login(request));
    }
}
