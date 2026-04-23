package edu.tcu.cs.projectpulse.peerevaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.rubric.CriterionEntity;
import edu.tcu.cs.projectpulse.rubric.RubricEntity;
import edu.tcu.cs.projectpulse.rubric.RubricRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PeerEvaluationControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired PeerEvaluationRepository peerEvaluationRepository;
    @Autowired UserRepository userRepository;
    @Autowired RubricRepository rubricRepository;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;
    UserEntity evaluator;
    UserEntity evaluatee;
    Long criterionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        peerEvaluationRepository.deleteAll();

        long ts = System.currentTimeMillis();

        evaluator = new UserEntity();
        evaluator.setFirstName("Eva");
        evaluator.setLastName("Luator");
        evaluator.setEmail("evaluator-" + ts + "@tcu.edu");
        evaluator.setRole("STUDENT");
        evaluator = userRepository.save(evaluator);

        evaluatee = new UserEntity();
        evaluatee.setFirstName("Eva");
        evaluatee.setLastName("Luatee");
        evaluatee.setEmail("evaluatee-" + ts + "@tcu.edu");
        evaluatee.setRole("STUDENT");
        evaluatee = userRepository.save(evaluatee);

        // Create a rubric with one criterion for testing
        RubricEntity rubric = new RubricEntity();
        rubric.setName("Test Rubric " + ts);
        rubric.setActive(true);
        CriterionEntity criterion = new CriterionEntity();
        criterion.setName("Teamwork");
        criterion.setDescription("Works well with others");
        criterion.setMaxScore(BigDecimal.TEN);
        rubric.addCriterion(criterion);
        rubric = rubricRepository.save(rubric);
        criterionId = rubric.getCriteria().get(0).getId();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> submitPayload(int week) {
        return Map.of(
                "week", week,
                "evaluations", List.of(Map.of(
                        "evaluateeId", evaluatee.getId(),
                        "criterionScores", List.of(Map.of("criterionId", criterionId, "score", 8)),
                        "publicComment", "Great collaborator",
                        "privateComment", "Could improve on deadlines"
                ))
        );
    }

    // ── GET /api/v1/peer-evaluations/my-submission ───────────────────────────

    @Test
    void mySubmission_notYetSubmitted_returnsNullData() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/my-submission?week=1&userId=" + evaluator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void mySubmission_afterSubmit_returnsSubmissionData() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitPayload(1))));

        mockMvc.perform(get("/api/v1/peer-evaluations/my-submission?week=1&userId=" + evaluator.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.week").value(1));
    }

    // ── POST /api/v1/peer-evaluations ────────────────────────────────────────

    @Test
    void submit_validPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitPayload(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submit_twice_returns409_BR3() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitPayload(1))));

        // Second submission for the same week must be rejected (BR-3)
        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitPayload(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_differentWeeks_bothSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitPayload(1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitPayload(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submit_missingWeek_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "evaluations", List.of(Map.of(
                        "evaluateeId", evaluatee.getId(),
                        "criterionScores", List.of(Map.of("criterionId", criterionId, "score", 8)),
                        "publicComment", "", "privateComment", ""
                ))
        );

        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_emptyEvaluations_returns400() throws Exception {
        Map<String, Object> payload = Map.of("week", 1, "evaluations", List.of());

        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submit_negativeScore_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "week", 1,
                "evaluations", List.of(Map.of(
                        "evaluateeId", evaluatee.getId(),
                        "criterionScores", List.of(Map.of("criterionId", criterionId, "score", -1)),
                        "publicComment", "", "privateComment", ""
                ))
        );

        mockMvc.perform(post("/api/v1/peer-evaluations?userId=" + evaluator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
