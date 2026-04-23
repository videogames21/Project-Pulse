package edu.tcu.cs.projectpulse.invitation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    Optional<InvitationEntity> findByToken(String token);
    List<InvitationEntity> findAllByOrderByCreatedAtDesc();
}
