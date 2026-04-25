package edu.tcu.cs.projectpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    List<UserEntity> findByRole(UserRole role);

    List<UserEntity> findByRoleAndNameContainingIgnoreCase(UserRole role, String name);

    List<UserEntity> findByRoleAndTeamIdIsNull(UserRole role);

    List<UserEntity> findByTeamId(Long teamId);

    List<UserEntity> findByRoleAndTeamId(UserRole role, Long teamId);
}
