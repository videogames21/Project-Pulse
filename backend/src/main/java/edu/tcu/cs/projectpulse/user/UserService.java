package edu.tcu.cs.projectpulse.user;

import edu.tcu.cs.projectpulse.auth.EmailAlreadyRegisteredException;
import edu.tcu.cs.projectpulse.auth.JwtService;
import edu.tcu.cs.projectpulse.auth.dto.AuthResponse;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.dto.ChangePasswordRequest;
import edu.tcu.cs.projectpulse.user.dto.InstructorDetailResponse;
import edu.tcu.cs.projectpulse.user.dto.UpdateProfileRequest;
import edu.tcu.cs.projectpulse.user.dto.UserProfileResponse;
import edu.tcu.cs.projectpulse.user.dto.UserResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       TeamRepository teamRepository,
                       SectionRepository sectionRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.sectionRepository = sectionRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
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

    public UserResponse findByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
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

    public UserProfileResponse getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String teamName = null;
        String sectionName = null;
        String instructorName = null;
        String instructorEmail = null;

        if (user.getRole() == UserRole.STUDENT && user.getTeamId() != null) {
            TeamEntity team = teamRepository.findById(user.getTeamId()).orElse(null);
            if (team != null) {
                teamName = team.getName();
                sectionName = team.getSectionName();

                SectionEntity section = sectionRepository.findByName(sectionName).orElse(null);
                if (section != null && section.getInstructorId() != null) {
                    UserEntity instructor = userRepository.findById(section.getInstructorId()).orElse(null);
                    if (instructor != null) {
                        instructorName = instructor.getName();
                        instructorEmail = instructor.getEmail();
                    }
                }
            }
        }

        return new UserProfileResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                user.getTeamId(), teamName, sectionName, instructorName, instructorEmail
        );
    }

    public AuthResponse updateProfile(String email, UpdateProfileRequest req) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!req.email().equals(user.getEmail())) {
            userRepository.findByEmail(req.email()).ifPresent(existing -> {
                throw new EmailAlreadyRegisteredException(req.email());
            });
        }

        String fullName = req.name().trim();
        int spaceIdx = fullName.indexOf(' ');
        if (spaceIdx > 0) {
            user.setFirstName(fullName.substring(0, spaceIdx));
            user.setLastName(fullName.substring(spaceIdx + 1));
        } else {
            user.setFirstName(fullName);
            user.setLastName("");
        }
        user.setEmail(req.email());
        userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(), user.getName(), user.getEmail(), user.getRole().name()
        );
    }

    public void changePassword(String email, ChangePasswordRequest req) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public List<UserResponse> findInstructors(String name) {
        List<UserEntity> instructors = (name != null && !name.isBlank())
                ? userRepository.findByRoleAndNameContaining(UserRole.INSTRUCTOR, name)
                : userRepository.findByRole(UserRole.INSTRUCTOR);
        return instructors.stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse toResponse(UserEntity entity) {
        String teamName = null;
        if (entity.getTeamId() != null) {
            teamName = teamRepository.findById(entity.getTeamId())
                    .map(TeamEntity::getName)
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
