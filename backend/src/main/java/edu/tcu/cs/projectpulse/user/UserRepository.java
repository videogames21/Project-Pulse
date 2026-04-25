package edu.tcu.cs.projectpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    List<UserEntity> findByRole(UserRole role);

    List<UserEntity> findByRoleAndStatus(UserRole role, UserStatus status);

    @Query("SELECT u FROM UserEntity u WHERE u.role = :role AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<UserEntity> findByRoleAndNameContaining(
            @Param("role") UserRole role,
            @Param("name") String name);

    @Query("SELECT u FROM UserEntity u WHERE u.role = :role AND u.status = :status AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<UserEntity> findByRoleAndStatusAndNameContaining(
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            @Param("name") String name);

    List<UserEntity> findByRoleAndTeamIdIsNull(UserRole role);

    List<UserEntity> findByTeamId(Long teamId);

    List<UserEntity> findByRoleAndTeamId(UserRole role, Long teamId);
}
