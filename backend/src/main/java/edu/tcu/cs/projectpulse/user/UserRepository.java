package edu.tcu.cs.projectpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    List<UserEntity> findByRole(UserRole role);

    List<UserEntity> findByRoleAndNameContainingIgnoreCase(UserRole role, String name);

    List<UserEntity> findByRoleAndTeamIdIsNull(UserRole role);

    List<UserEntity> findByTeamId(Long teamId);

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByInvitationToken(String invitationToken);
}
