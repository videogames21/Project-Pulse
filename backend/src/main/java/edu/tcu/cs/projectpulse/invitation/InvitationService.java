package edu.tcu.cs.projectpulse.invitation;

import edu.tcu.cs.projectpulse.invitation.dto.InvitationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvitationService {

    private final InvitationRepository repository;

    @Value("${app.base-url}")
    private String baseUrl;

    // TODO: Replace with JWT principal once auth module is built
    @Value("${app.admin-email}")
    private String adminEmail;

    public InvitationService(InvitationRepository repository) {
        this.repository = repository;
    }

    public InvitationResponse generateInvitation() {
        InvitationEntity entity = new InvitationEntity();
        entity.setToken(UUID.randomUUID().toString());
        entity.setStatus(InvitationStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setInvitedBy(adminEmail);
        entity = repository.save(entity);
        return toResponse(entity, true);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(e -> toResponse(e, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvitationResponse findByToken(String token) {
        InvitationEntity entity = repository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException(token));
        return toResponse(entity, false);
    }

    private InvitationResponse toResponse(InvitationEntity entity, boolean includeLink) {
        InvitationResponse dto = new InvitationResponse();
        dto.setId(entity.getId());
        dto.setTokenShort(entity.getToken().substring(0, 8));
        dto.setStatus(entity.getStatus().name());
        dto.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setInvitedBy(entity.getInvitedBy());
        if (includeLink) {
            dto.setRegistrationLink(baseUrl + "/register/" + entity.getToken());
        }
        return dto;
    }
}
