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

import edu.tcu.cs.projectpulse.section.SectionNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<UserResponse> findUnassignedStudents(Long sectionId) {
        List<UserEntity> students = sectionId != null
                ? userRepository.findByRoleAndTeamIdIsNullAndSectionId(UserRole.STUDENT, sectionId)
                : userRepository.findByRoleAndTeamIdIsNull(UserRole.STUDENT);
        return students.stream().map(this::toResponse).toList();
    }

    public UserProfileResponse getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String teamName = null;
        String sectionName = null;
        String instructorName = null;
        String instructorEmail = null;

        if (user.getRole() == UserRole.STUDENT) {
            if (user.getTeamId() != null) {
                teamName = teamRepository.findById(user.getTeamId())
                        .map(TeamEntity::getName).orElse(null);
            }
            if (user.getSectionId() != null) {
                SectionEntity section = sectionRepository.findById(user.getSectionId()).orElse(null);
                if (section != null) {
                    sectionName = section.getName();
                    List<UserEntity> sectionInstructors =
                            userRepository.findByRoleAndSectionId(UserRole.INSTRUCTOR, section.getId());
                    if (!sectionInstructors.isEmpty()) {
                        UserEntity instructor = sectionInstructors.get(0);
                        instructorName = instructor.getName();
                        instructorEmail = instructor.getEmail();
                    }
                }
            }
        }

        if (user.getRole() == UserRole.INSTRUCTOR) {
            String supervisedSectionName = user.getSectionId() != null
                    ? sectionRepository.findById(user.getSectionId())
                            .map(SectionEntity::getName).orElse(null)
                    : null;
            UserEntity admin = userRepository.findByRole(UserRole.ADMIN)
                    .stream().findFirst().orElse(null);
            String adminName  = admin != null ? admin.getName()  : null;
            String adminEmail = admin != null ? admin.getEmail() : null;
            return new UserProfileResponse(
                    user.getId(), user.getName(),
                    user.getFirstName(), user.getLastName(), user.getMiddleInitial(),
                    user.getEmail(), user.getRole().name(),
                    null, null, null, null, null,
                    supervisedSectionName, adminName, adminEmail
            );
        }

        return new UserProfileResponse(
                user.getId(), user.getName(),
                user.getFirstName(), user.getLastName(), user.getMiddleInitial(),
                user.getEmail(), user.getRole().name(),
                user.getTeamId(), teamName, sectionName, instructorName, instructorEmail,
                null, null, null
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

        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setMiddleInitial(req.middleInitial());
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
        if (entity.getRole() == UserRole.INSTRUCTOR) {
            List<TeamEntity> teams = teamRepository.findTeamsByInstructorId(entity.getId());
            if (!teams.isEmpty()) {
                teamName = teams.stream().map(TeamEntity::getName).collect(java.util.stream.Collectors.joining(", "));
            }
        } else if (entity.getTeamId() != null) {
            teamName = teamRepository.findById(entity.getTeamId())
                    .map(TeamEntity::getName)
                    .orElse(null);
        }
        String supervisedSectionName = null;
        if (entity.getRole() == UserRole.INSTRUCTOR) {
            supervisedSectionName = entity.getSectionId() != null
                    ? sectionRepository.findById(entity.getSectionId())
                            .map(SectionEntity::getName).orElse(null)
                    : null;
        }
        return new UserResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRole().name(),
                entity.getStatus().name(),
                entity.getTeamId(),
                teamName,
                supervisedSectionName
        );
    }

    @Transactional
    public InstructorDetailResponse deactivateInstructor(Long id, String reason) {
        UserEntity user = findEntityById(id);
        if (user.getRole() != UserRole.INSTRUCTOR) {
            throw new IllegalArgumentException("User " + id + " is not an instructor");
        }
        user.setStatus(UserStatus.DEACTIVATED);
        user.setDeactivationReason(reason);
        teamRepository.findTeamsByInstructorId(id).forEach(team -> {
            team.getInstructors().removeIf(i -> i.getId().equals(id));
            teamRepository.save(team);
        });
        user.setSectionId(null);
        userRepository.save(user);
        return toInstructorDetail(user);
    }

    @Transactional
    public InstructorDetailResponse assignSection(Long userId, Long sectionId) {
        UserEntity user = findEntityById(userId);
        if (user.getRole() != UserRole.INSTRUCTOR) {
            throw new IllegalArgumentException("User " + userId + " is not an instructor");
        }
        if (sectionId != null) {
            sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new SectionNotFoundException(sectionId));
        }
        user.setSectionId(sectionId);
        userRepository.save(user);
        return toInstructorDetail(user);
    }

    @Transactional
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
        List<InstructorDetailResponse.SupervisedTeam> supervisedTeams =
                teamRepository.findTeamsByInstructorId(user.getId()).stream()
                        .map(t -> new InstructorDetailResponse.SupervisedTeam(
                                t.getId(), t.getName(), t.getSectionName()))
                        .toList();

        String supervisedSectionName = user.getSectionId() != null
                ? sectionRepository.findById(user.getSectionId())
                        .map(SectionEntity::getName).orElse(null)
                : null;

        return new InstructorDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                supervisedTeams,
                supervisedSectionName,
                user.getSectionId()
        );
    }
}
