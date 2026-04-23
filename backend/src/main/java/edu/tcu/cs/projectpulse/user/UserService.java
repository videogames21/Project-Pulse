package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.user.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse findById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    public List<UserResponse> findStudents() {
        return userRepository.findByRole(UserRole.STUDENT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserResponse> findUnassignedStudents() {
        return userRepository.findByRoleAndTeamIdIsNull(UserRole.STUDENT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getRole().name(),
                entity.getTeamId()
        );
    }
}
