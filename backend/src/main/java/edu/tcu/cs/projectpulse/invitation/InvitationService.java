package edu.tcu.cs.projectpulse.invitation;

import edu.tcu.cs.projectpulse.invitation.dto.AcceptedUserInfo;
import edu.tcu.cs.projectpulse.invitation.dto.InvitationResponse;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionNotFoundException;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);
    private static final String ACCESS_CODE_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final InvitationRepository repository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public InvitationService(InvitationRepository repository, UserRepository userRepository,
                             SectionRepository sectionRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
    }

    public InvitationResponse generateInvitation(Long sectionId) {
        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));
        String invitedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        InvitationEntity entity = new InvitationEntity();
        entity.setToken(UUID.randomUUID().toString());
        entity.setStatus(InvitationStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setInvitedBy(invitedBy);
        entity.setRole(UserRole.STUDENT);
        entity.setSectionId(section.getId());
        entity = repository.save(entity);
        return toResponse(entity);
    }

    public InvitationResponse generateInstructorInvitation(Long sectionId) {
        String invitedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        InvitationEntity entity = new InvitationEntity();
        entity.setToken(UUID.randomUUID().toString());
        entity.setStatus(InvitationStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setInvitedBy(invitedBy);
        entity.setRole(UserRole.INSTRUCTOR);
        entity.setAccessCode(generateAccessCode());
        if (sectionId != null) {
            sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new SectionNotFoundException(sectionId));
            entity.setSectionId(sectionId);
        }
        entity = repository.save(entity);
        log.info("Instructor invitation created: token={}", entity.getToken());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvitationResponse findByToken(String token) {
        InvitationEntity entity = repository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException(token));
        return toResponse(entity);
    }

    public InvitationResponse disableInvitation(String token) {
        InvitationEntity entity = repository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException(token));
        if (entity.getStatus() == InvitationStatus.DISABLED) {
            throw new IllegalStateException("Invitation is already disabled.");
        }
        entity.setStatus(InvitationStatus.DISABLED);
        repository.save(entity);
        return toResponse(entity);
    }

    public InvitationResponse enableInvitation(String token) {
        InvitationEntity entity = repository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException(token));
        if (entity.getStatus() == InvitationStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot re-enable a used instructor invitation.");
        }
        if (entity.getStatus() == InvitationStatus.ACTIVE) {
            throw new IllegalStateException("Invitation is already active.");
        }
        entity.setStatus(InvitationStatus.ACTIVE);
        repository.save(entity);
        return toResponse(entity);
    }

    public void deleteInvitation(String token) {
        InvitationEntity entity = repository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException(token));
        if (entity.getStatus() == InvitationStatus.ACTIVE) {
            throw new IllegalStateException("You must first disable the invitation link before deleting it.");
        }
        repository.delete(entity);
    }

    private InvitationResponse toResponse(InvitationEntity entity) {
        List<UserEntity> users = userRepository.findByInvitationToken(entity.getToken());
        List<AcceptedUserInfo> acceptedUsers = users.stream()
                .map(u -> new AcceptedUserInfo(u.getName(), u.getEmail()))
                .toList();

        String sectionName = null;
        if (entity.getSectionId() != null) {
            sectionName = sectionRepository.findById(entity.getSectionId())
                    .map(SectionEntity::getName).orElse(null);
        }

        InvitationResponse dto = new InvitationResponse();
        dto.setId(entity.getId());
        dto.setToken(entity.getToken());
        dto.setTokenShort(entity.getToken().substring(0, 8));
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setInvitedBy(entity.getInvitedBy());
        dto.setEmail(entity.getEmail());
        dto.setRegistrationLink(baseUrl + "/register/" + entity.getToken());
        dto.setUsageCount(acceptedUsers.size());
        dto.setAcceptedUsers(acceptedUsers);
        dto.setRole(entity.getRole() != null ? entity.getRole().name() : "STUDENT");
        dto.setAccessCode(entity.getAccessCode());
        dto.setSectionId(entity.getSectionId());
        dto.setSectionName(sectionName);
        return dto;
    }

    private String generateAccessCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ACCESS_CODE_CHARS.charAt(rng.nextInt(ACCESS_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
