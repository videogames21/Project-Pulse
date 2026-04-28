package edu.tcu.cs.projectpulse.team;

import edu.tcu.cs.projectpulse.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<TeamEntity, Long>, JpaSpecificationExecutor<TeamEntity> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<TeamEntity> findAllBySectionNameOrderByNameAsc(String sectionName);

    @Query("SELECT u FROM TeamEntity t JOIN t.instructors u WHERE t.id = :teamId")
    List<UserEntity> findInstructorsByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT t FROM TeamEntity t JOIN t.instructors u WHERE u.id = :instructorId")
    List<TeamEntity> findTeamsByInstructorId(@Param("instructorId") Long instructorId);
}
