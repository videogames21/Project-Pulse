package edu.tcu.cs.projectpulse.peerevaluation;

import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import edu.tcu.cs.projectpulse.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class StudentPeerEvalRangeReportIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired PeerEvaluationRepository peerEvaluationRepository;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;

    MockMvc mockMvc;

    private static final String WEEK1 = "2024-09-02";
    private static final String WEEK2 = "2024-09-09";
    private static final String WEEK3 = "2024-09-16";

    Long teamId;
    Long aliceId;
    Long bobId;

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

        aliceId = createStudent("Alice", "Johnson", "alice@tcu.edu");
        bobId   = createStudent("Bob",   "Smith",   "bob@tcu.edu");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long createStudent(String firstName, String lastName, String email) {
        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setRole(UserRole.STUDENT);
        u.setStatus(UserStatus.ACTIVE);
        u.setTeamId(teamId);
        return userRepository.save(u).getId();
    }

    private void submitEval(Long evaluatorId, Long evaluateeId, String weekStart,
                            String publicComment, String privateComment) {
        PeerEvaluationEntity eval = new PeerEvaluationEntity();
        eval.setEvaluatorId(evaluatorId);
        eval.setEvaluateeId(evaluateeId);
        eval.setWeekStart(LocalDate.parse(weekStart));
        eval.setPublicComments(publicComment);
        eval.setPrivateComments(privateComment);
        PeerEvaluationScoreEntity score = new PeerEvaluationScoreEntity();
        score.setPeerEvaluation(eval);
        score.setCriterionId(1L);
        score.setScore(8);
        eval.getScores().add(score);
        peerEvaluationRepository.save(eval);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void getRangeReport_noEvals_returnsEmptyWeeksList() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(aliceId))
                .andExpect(jsonPath("$.data.startWeek").value(WEEK1))
                .andExpect(jsonPath("$.data.endWeek").value(WEEK3))
                .andExpect(jsonPath("$.data.weeks", hasSize(0)));
    }

    @Test
    void getRangeReport_evalsInRange_groupedByWeek() throws Exception {
        submitEval(bobId, aliceId, WEEK1, "Good work.", "Needs follow-through.");
        submitEval(bobId, aliceId, WEEK2, "Improved.", "Keep it up.");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks", hasSize(2)))
                .andExpect(jsonPath("$.data.weeks[0].weekStart").value(WEEK1))
                .andExpect(jsonPath("$.data.weeks[1].weekStart").value(WEEK2));
    }

    @Test
    void getRangeReport_sortedChronologically() throws Exception {
        submitEval(bobId, aliceId, WEEK2, "Week 2 comment.", "Private 2.");
        submitEval(bobId, aliceId, WEEK1, "Week 1 comment.", "Private 1.");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].weekStart").value(WEEK1))
                .andExpect(jsonPath("$.data.weeks[1].weekStart").value(WEEK2));
    }

    @Test
    void getRangeReport_includesPrivateComments() throws Exception {
        submitEval(bobId, aliceId, WEEK1, "Nice.", "Private note.");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].receivedEvaluations[0].privateComments").value("Private note."))
                .andExpect(jsonPath("$.data.weeks[0].receivedEvaluations[0].publicComments").value("Nice."));
    }

    @Test
    void getRangeReport_includesEvaluatorName() throws Exception {
        submitEval(bobId, aliceId, WEEK1, "Good.", "");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].receivedEvaluations[0].evaluatorName").value("Bob Smith"));
    }

    @Test
    void getRangeReport_excludesEvalsOutsideRange() throws Exception {
        submitEval(bobId, aliceId, WEEK1, "In range.", "");
        submitEval(bobId, aliceId, WEEK3, "Out of range.", "");

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks", hasSize(1)))
                .andExpect(jsonPath("$.data.weeks[0].weekStart").value(WEEK1));
    }

    @Test
    void getRangeReport_userIsNotAStudent_returns400() throws Exception {
        UserEntity instructor = new UserEntity();
        instructor.setFirstName("Prof");
        instructor.setLastName("Jones");
        instructor.setEmail("jones@tcu.edu");
        instructor.setRole(UserRole.INSTRUCTOR);
        Long instructorId = userRepository.save(instructor).getId();

        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + instructorId + "/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRangeReport_studentNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/students/9999/report/range")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRangeReport_startWeekNotMonday_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", "2024-09-03")
                        .param("endWeek", WEEK2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRangeReport_endBeforeStart_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/students/" + aliceId + "/report/range")
                        .param("startWeek", WEEK3)
                        .param("endWeek", WEEK1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
