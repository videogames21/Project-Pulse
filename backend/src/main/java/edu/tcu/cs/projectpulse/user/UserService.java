package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.dto.InstructorDetailResponse;
import edu.tcu.cs.projectpulse.user.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public UserService(UserRepository userRepository, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    public UserEntity findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public InstructorDetailResponse findInstructorById(Long id) {
        UserEntity user = findEntityById(id);
        return toInstructorDetail(user);
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

    public List<UserResponse> findInstructors(String name, UserStatus status) {
        List<UserEntity> instructors;
        boolean hasName   = name != null && !name.isBlank();
        boolean hasStatus = status != null;

        if (hasName && hasStatus) {
            instructors = userRepository.findByRoleAndStatusAndNameContaining(UserRole.INSTRUCTOR, status, name);
        } else if (hasName) {
            instructors = userRepository.findByRoleAndNameContaining(UserRole.INSTRUCTOR, name);
        } else if (hasStatus) {
            instructors = userRepository.findByRoleAndStatus(UserRole.INSTRUCTOR, status);
        } else {
            instructors = userRepository.findByRole(UserRole.INSTRUCTOR);
        }
        return instructors.stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse toResponse(UserEntity entity) {
        String teamName = null;
        if (entity.getTeamId() != null) {
            teamName = teamRepository.findById(entity.getTeamId())
                    .map(t -> t.getName())
                    .orElse(null);
        }
        return new UserResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRole().name(),
                entity.getStatus().name(),
                entity.getTeamId(),
                teamName
        );
    }

    public InstructorDetailResponse deactivateInstructor(Long id, String reason) {
        UserEntity user = findEntityById(id);
        if (user.getRole() != UserRole.INSTRUCTOR) {
            throw new IllegalArgumentException("User " + id + " is not an instructor");
        }
        user.setStatus(UserStatus.DEACTIVATED);
        user.setDeactivationReason(reason);
        user.setTeamId(null);
        userRepository.save(user);
        return toInstructorDetail(user);
    }

    public InstructorDetailResponse reactivateInstructor(Long id) {
        UserEntity user = findEntityById(id);
        if (user.getRole() != UserRole.INSTRUCTOR) {
            throw new IllegalArgumentException("User " + id + " is not an instructor");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setDeactivationReason(null);
        userRepository.save(user);
        return toInstructorDetail(user);
    }

    public InstructorDetailResponse toInstructorDetail(UserEntity user) {
        InstructorDetailResponse.SupervisedTeam supervisedTeam = null;
        if (user.getTeamId() != null) {
            Optional<TeamEntity> teamOpt = teamRepository.findById(user.getTeamId());
            if (teamOpt.isPresent()) {
                TeamEntity team = teamOpt.get();
                supervisedTeam = new InstructorDetailResponse.SupervisedTeam(
                        team.getId(), team.getName(), team.getSectionName());
            }
        }
        return new InstructorDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                supervisedTeam
        );
    }
}
