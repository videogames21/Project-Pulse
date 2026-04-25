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
class StudentPeerEvaluationReportIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired PeerEvaluationRepository peerEvaluationRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    private static final String PAST_MONDAY = "2024-09-02";
    private static final String ANOTHER_MONDAY = "2024-09-09";

    Long teamId;
    Long evaluateeId;

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

        UserEntity evaluatee = new UserEntity();
        evaluatee.setName("Test Student A");
        evaluatee.setEmail("student_a@tcu.edu");
        evaluatee.setRole(UserRole.STUDENT);
        evaluatee.setTeamId(teamId);
        evaluateeId = userRepository.save(evaluatee).getId();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long createEvaluator(String name, String email) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(UserRole.STUDENT);
        u.setTeamId(teamId);
        return userRepository.save(u).getId();
    }

    private void submitEval(Long evaluatorId, Long evalEvalId, String weekStart,
                            List<Map<String, Object>> scores,
                            String publicComments, String privateComments) throws Exception {
        var body = Map.of(
                "evaluatorId", evaluatorId,
                "evaluateeId", evalEvalId,
                "weekStart", weekStart,
                "scores", scores,
                "publicComments", publicComments,
                "privateComments", privateComments
        );
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private List<Map<String, Object>> scores(int s1, int s2) {
        return List.of(
                Map.of("criterionId", 1L, "score", s1),
                Map.of("criterionId", 2L, "score", s2)
        );
    }

    // ── GET /api/v1/peer-evaluations/students/{id}/report?weekStart=... ──────

    @Test
    void getReport_noEvaluations_returns200WithEmptyReport() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(evaluateeId))
                .andExpect(jsonPath("$.data.studentName").value("Test Student A"))
                .andExpect(jsonPath("$.data.weekStart").value(PAST_MONDAY))
                .andExpect(jsonPath("$.data.evaluationCount").value(0))
                .andExpect(jsonPath("$.data.averageCriterionScores", hasSize(0)))
                .andExpect(jsonPath("$.data.publicComments", hasSize(0)))
                .andExpect(jsonPath("$.data.averageGrade").value(0));
    }

    @Test
    void getReport_singleEvaluation_returnsScoresAndComments() throws Exception {
        Long evaluatorId = createEvaluator("Evaluator B", "eval_b@tcu.edu");
        submitEval(evaluatorId, evaluateeId, PAST_MONDAY, scores(8, 9), "Great work!", "Private note.");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationCount").value(1))
                .andExpect(jsonPath("$.data.averageCriterionScores", hasSize(2)))
                .andExpect(jsonPath("$.data.averageCriterionScores[0].criterionId").value(1))
                .andExpect(jsonPath("$.data.averageCriterionScores[0].averageScore").value(8.00))
                .andExpect(jsonPath("$.data.averageCriterionScores[1].criterionId").value(2))
                .andExpect(jsonPath("$.data.averageCriterionScores[1].averageScore").value(9.00))
                .andExpect(jsonPath("$.data.publicComments", hasSize(1)))
                .andExpect(jsonPath("$.data.publicComments[0]").value("Great work!"))
                .andExpect(jsonPath("$.data.averageGrade").value(17.00));
    }

    @Test
    void getReport_multipleEvaluations_averagesAllScores() throws Exception {
        // Evaluator B: criterion1=8, criterion2=10  → total=18
        // Evaluator C: criterion1=6, criterion2=8   → total=14
        // averages: criterion1=(8+6)/2=7.00, criterion2=(10+8)/2=9.00, grade=(18+14)/2=16.00
        Long evaluatorB = createEvaluator("Evaluator B", "eval_b@tcu.edu");
        Long evaluatorC = createEvaluator("Evaluator C", "eval_c@tcu.edu");

        submitEval(evaluatorB, evaluateeId, PAST_MONDAY, scores(8, 10), "Nice job.", "Note from B.");
        submitEval(evaluatorC, evaluateeId, PAST_MONDAY, scores(6, 8),  "Keep it up.", "Note from C.");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationCount").value(2))
                .andExpect(jsonPath("$.data.averageCriterionScores[0].averageScore").value(7.00))
                .andExpect(jsonPath("$.data.averageCriterionScores[1].averageScore").value(9.00))
                .andExpect(jsonPath("$.data.publicComments", hasSize(2)))
                .andExpect(jsonPath("$.data.averageGrade").value(16.00));
    }

    @Test
    void getReport_doesNotExposePrivateCommentsOrEvaluatorIds() throws Exception {
        Long evaluatorId = createEvaluator("Evaluator B", "eval_b@tcu.edu");
        submitEval(evaluatorId, evaluateeId, PAST_MONDAY, scores(8, 9), "Public note.", "SECRET.");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privateComments").doesNotExist())
                .andExpect(jsonPath("$.data.evaluatorId").doesNotExist());
    }

    @Test
    void getReport_onlyCountsEvaluationsForThatWeek() throws Exception {
        Long evaluatorId = createEvaluator("Evaluator B", "eval_b@tcu.edu");
        submitEval(evaluatorId, evaluateeId, PAST_MONDAY,    scores(8, 9), "Week 1.", "");
        submitEval(evaluatorId, evaluateeId, ANOTHER_MONDAY, scores(4, 5), "Week 2.", "");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationCount").value(1))
                .andExpect(jsonPath("$.data.weekStart").value(PAST_MONDAY));
    }

    @Test
    void getReport_studentNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/students/9999/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getReport_weekStartNotMonday_returns400() throws Exception {
        // 2024-09-03 is a Tuesday
        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", "2024-09-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getReport_blankPublicCommentExcludedFromList() throws Exception {
        Long evaluatorB = createEvaluator("Evaluator B", "eval_b@tcu.edu");
        Long evaluatorC = createEvaluator("Evaluator C", "eval_c@tcu.edu");

        submitEval(evaluatorB, evaluateeId, PAST_MONDAY, scores(8, 9), "Good work.", "");
        submitEval(evaluatorC, evaluateeId, PAST_MONDAY, scores(7, 8), "",            "");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + evaluateeId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluationCount").value(2))
                .andExpect(jsonPath("$.data.publicComments", hasSize(1)))
                .andExpect(jsonPath("$.data.publicComments[0]").value("Good work."));
    }
}
