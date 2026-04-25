package edu.tcu.cs.projectpulse.peerevaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PeerEvaluationControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired PeerEvaluationRepository peerEvaluationRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    // A past Monday
    private static final String PAST_MONDAY = "2024-09-02";
    private static final String ANOTHER_MONDAY = "2024-09-09";
    private static final String FUTURE_MONDAY = "2099-01-06";

    Long teamId;
    Long evaluatorId;
    Long evaluateeId;
    Long criterionId1 = 1L;
    Long criterionId2 = 2L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        peerEvaluationRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        TeamEntity team = new TeamEntity();
        team.setName("Team Alpha");
        team.setSectionName("CS4910");
        teamId = teamRepository.save(team).getId();

        UserEntity evaluator = new UserEntity();
        evaluator.setName("Test Student A");
        evaluator.setEmail("alice@tcu.edu");
        evaluator.setRole(UserRole.STUDENT);
        evaluator.setTeamId(teamId);
        evaluatorId = userRepository.save(evaluator).getId();

        UserEntity evaluatee = new UserEntity();
        evaluatee.setName("Bob Smith");
        evaluatee.setEmail("bob@tcu.edu");
        evaluatee.setRole(UserRole.STUDENT);
        evaluatee.setTeamId(teamId);
        evaluateeId = userRepository.save(evaluatee).getId();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> validBody(Long evaluatorId, Long evaluateeId, String weekStart) {
        return Map.of(
                "evaluatorId", evaluatorId,
                "evaluateeId", evaluateeId,
                "weekStart", weekStart,
                "scores", List.of(
                        Map.of("criterionId", criterionId1, "score", 8),
                        Map.of("criterionId", criterionId2, "score", 9)
                ),
                "publicComments", "Great work!",
                "privateComments", "Needs improvement in communication."
        );
    }

    private Long submitEval(Long evaluatorId, Long evaluateeId, String weekStart) throws Exception {
        String response = mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluateeId, weekStart))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").longValue();
    }

    // ── POST /api/v1/peer-evaluations ────────────────────────────────────────

    @Test
    void submit_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluateeId, PAST_MONDAY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.evaluatorId").value(evaluatorId))
                .andExpect(jsonPath("$.data.evaluateeId").value(evaluateeId))
                .andExpect(jsonPath("$.data.weekStart").value(PAST_MONDAY))
                .andExpect(jsonPath("$.data.scores", hasSize(2)))
                .andExpect(jsonPath("$.data.publicComments").value("Great work!"))
                .andExpect(jsonPath("$.data.privateComments").value("Needs improvement in communication."));
    }

    @Test
    void submit_selfEvaluation_isAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluatorId, PAST_MONDAY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submit_duplicate_returns409() throws Exception {
        submitEval(evaluatorId, evaluateeId, PAST_MONDAY);

        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluateeId, PAST_MONDAY))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_futureWeek_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluateeId, FUTURE_MONDAY))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_weekStartNotMonday_returns400() throws Exception {
        // 2024-09-03 is a Tuesday
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluateeId, "2024-09-03"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_evaluatorNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(9999L, evaluateeId, PAST_MONDAY))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_evaluateeNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, 9999L, PAST_MONDAY))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_evaluatorNotOnTeam_returns409() throws Exception {
        UserEntity unassigned = new UserEntity();
        unassigned.setName("Carol White");
        unassigned.setEmail("carol@tcu.edu");
        unassigned.setRole(UserRole.STUDENT);
        Long unassignedId = userRepository.save(unassigned).getId();

        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(unassignedId, evaluateeId, PAST_MONDAY))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_differentTeams_returns409() throws Exception {
        TeamEntity otherTeam = new TeamEntity();
        otherTeam.setName("Team Beta");
        otherTeam.setSectionName("CS4910");
        Long otherTeamId = teamRepository.save(otherTeam).getId();

        UserEntity otherStudent = new UserEntity();
        otherStudent.setName("Dave Brown");
        otherStudent.setEmail("dave@tcu.edu");
        otherStudent.setRole(UserRole.STUDENT);
        otherStudent.setTeamId(otherTeamId);
        Long otherStudentId = userRepository.save(otherStudent).getId();

        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, otherStudentId, PAST_MONDAY))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_missingScores_returns400() throws Exception {
        var body = Map.of(
                "evaluatorId", evaluatorId,
                "evaluateeId", evaluateeId,
                "weekStart", PAST_MONDAY,
                "scores", List.of()
        );

        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/v1/peer-evaluations/{id} ────────────────────────────────────

    @Test
    void findById_found_returns200() throws Exception {
        Long id = submitEval(evaluatorId, evaluateeId, PAST_MONDAY);

        mockMvc.perform(get("/api/v1/peer-evaluations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.scores", hasSize(2)));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PUT /api/v1/peer-evaluations/{id} ────────────────────────────────────

    @Test
    void update_validRequest_returns200() throws Exception {
        Long id = submitEval(evaluatorId, evaluateeId, PAST_MONDAY);

        var updateBody = Map.of(
                "evaluatorId", evaluatorId,
                "evaluateeId", evaluateeId,
                "weekStart", PAST_MONDAY,
                "scores", List.of(
                        Map.of("criterionId", criterionId1, "score", 10),
                        Map.of("criterionId", criterionId2, "score", 10)
                ),
                "publicComments", "Updated comments",
                "privateComments", "Updated private"
        );

        mockMvc.perform(put("/api/v1/peer-evaluations/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicComments").value("Updated comments"))
                .andExpect(jsonPath("$.data.scores[0].score").isNumber());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/peer-evaluations/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(evaluatorId, evaluateeId, PAST_MONDAY))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
