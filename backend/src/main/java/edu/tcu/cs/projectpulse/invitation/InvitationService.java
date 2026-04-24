package edu.tcu.cs.projectpulse.invitation;

import edu.tcu.cs.projectpulse.invitation.dto.AcceptedUserInfo;
import edu.tcu.cs.projectpulse.invitation.dto.InvitationResponse;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvitationService {

    private final InvitationRepository repository;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public InvitationService(InvitationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public InvitationResponse generateInvitation() {
        String invitedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        InvitationEntity entity = new InvitationEntity();
        entity.setToken(UUID.randomUUID().toString());
        entity.setStatus(InvitationStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setInvitedBy(invitedBy);
        entity = repository.save(entity);
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

        InvitationResponse dto = new InvitationResponse();
        dto.setId(entity.getId());
        dto.setToken(entity.getToken());
        dto.setTokenShort(entity.getToken().substring(0, 8));
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setInvitedBy(entity.getInvitedBy());
        dto.setRegistrationLink(baseUrl + "/register/" + entity.getToken());
        dto.setUsageCount(acceptedUsers.size());
        dto.setAcceptedUsers(acceptedUsers);
        return dto;
    }
}
