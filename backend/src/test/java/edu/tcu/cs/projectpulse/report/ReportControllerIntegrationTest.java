package edu.tcu.cs.projectpulse.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.peerevaluation.CriterionScoreEntity;
import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationEntity;
import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationRepository;
import edu.tcu.cs.projectpulse.rubric.CriterionEntity;
import edu.tcu.cs.projectpulse.rubric.RubricEntity;
import edu.tcu.cs.projectpulse.rubric.RubricRepository;
import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.war.WAREntryEntity;
import edu.tcu.cs.projectpulse.war.WAREntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ReportControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired PeerEvaluationRepository peerEvaluationRepository;
    @Autowired WAREntryRepository warEntryRepository;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired RubricRepository rubricRepository;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;

    UserEntity student1;
    UserEntity student2;
    TeamEntity team;
    CriterionEntity criterion;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        peerEvaluationRepository.deleteAll();
        warEntryRepository.deleteAll();

        long ts = System.currentTimeMillis();

        student1 = new UserEntity();
        student1.setFirstName("Alice");
        student1.setLastName("Report");
        student1.setEmail("report-s1-" + ts + "@tcu.edu");
        student1.setRole("STUDENT");
        student1 = userRepository.save(student1);

        student2 = new UserEntity();
        student2.setFirstName("Bob");
        student2.setLastName("Report");
        student2.setEmail("report-s2-" + ts + "@tcu.edu");
        student2.setRole("STUDENT");
        student2 = userRepository.save(student2);

        team = new TeamEntity();
        team.setName("Report Team " + ts);
        team.setSectionName("CS4910");
        team.setMembers(List.of(student1, student2));
        team = teamRepository.save(team);

        RubricEntity rubric = new RubricEntity();
        rubric.setName("Report Rubric " + ts);
        rubric.setActive(true);
        CriterionEntity c = new CriterionEntity();
        c.setName("Teamwork");
        c.setDescription("Works well with others");
        c.setMaxScore(BigDecimal.TEN);
        rubric.addCriterion(c);
        rubric = rubricRepository.save(rubric);
        criterion = rubric.getCriteria().get(0);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void savePeerEval(UserEntity evaluator, UserEntity evaluatee, int week, int score) {
        PeerEvaluationEntity eval = new PeerEvaluationEntity();
        eval.setEvaluator(evaluator);
        eval.setEvaluatee(evaluatee);
        eval.setWeek(week);
        eval.setPublicComment("Good work");
        eval.setPrivateComment("Private note");

        CriterionScoreEntity cs = new CriterionScoreEntity();
        cs.setCriterion(criterion);
        cs.setScore(score);
        eval.addCriterionScore(cs);

        peerEvaluationRepository.save(eval);
    }

    private void saveWAREntry(UserEntity student, int week, double planned, double actual) {
        WAREntryEntity entry = new WAREntryEntity();
        entry.setStudent(student);
        entry.setWeek(week);
        entry.setCategory("DEVELOPMENT");
        entry.setDescription("Some task");
        entry.setPlannedHours(BigDecimal.valueOf(planned));
        entry.setActualHours(BigDecimal.valueOf(actual));
        entry.setStatus("Done");
        warEntryRepository.save(entry);
    }

    // ── UC-29: GET /api/v1/reports/peer-eval/student/me ─────────────────────

    @Test
    void myPeerEvalReport_noEvaluations_returnsEmptyReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/peer-eval/student/me?week=1&userId=" + student1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.week").value(1))
                .andExpect(jsonPath("$.data.criteria", hasSize(0)))
                .andExpect(jsonPath("$.data.publicComments", hasSize(0)));
    }

    @Test
    void myPeerEvalReport_withEvaluations_returnsCriterionAverages() throws Exception {
        savePeerEval(student2, student1, 1, 8);

        mockMvc.perform(get("/api/v1/reports/peer-eval/student/me?week=1&userId=" + student1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore").value(8.0))
                .andExpect(jsonPath("$.data.maxScore").value(10.0))
                .andExpect(jsonPath("$.data.criteria", hasSize(1)))
                .andExpect(jsonPath("$.data.criteria[0].name").value("Teamwork"))
                .andExpect(jsonPath("$.data.criteria[0].avgScore").value(8.0))
                .andExpect(jsonPath("$.data.publicComments", hasSize(1)))
                .andExpect(jsonPath("$.data.publicComments[0]").value("Good work"));
    }

    @Test
    void myPeerEvalReport_multipleEvaluators_returnsCorrectAverage() throws Exception {
        UserEntity student3 = new UserEntity();
        student3.setFirstName("Carol");
        student3.setLastName("Report");
        student3.setEmail("report-s3-" + System.currentTimeMillis() + "@tcu.edu");
        student3.setRole("STUDENT");
        student3 = userRepository.save(student3);

        savePeerEval(student2, student1, 1, 6);
        savePeerEval(student3, student1, 1, 10);

        mockMvc.perform(get("/api/v1/reports/peer-eval/student/me?week=1&userId=" + student1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].avgScore").value(8.0)); // (6+10)/2
    }

    // ── UC-31: GET /api/v1/reports/peer-eval/section ────────────────────────

    @Test
    void sectionPeerEvalReport_returnsAllStudents() throws Exception {
        mockMvc.perform(get("/api/v1/reports/peer-eval/section?week=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].firstName",
                        hasItems("Alice", "Bob")));
    }

    @Test
    void sectionPeerEvalReport_studentWithSubmission_showsSubmittedTrue() throws Exception {
        savePeerEval(student1, student2, 1, 7);

        mockMvc.perform(get("/api/v1/reports/peer-eval/section?week=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.firstName=='Alice')].submitted", contains(true)));
    }

    @Test
    void sectionPeerEvalReport_studentWithNoSubmission_showsSubmittedFalse() throws Exception {
        mockMvc.perform(get("/api/v1/reports/peer-eval/section?week=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.firstName=='Alice')].submitted", contains(false)));
    }

    // ── UC-32: GET /api/v1/reports/war/team/{id} ─────────────────────────────

    @Test
    void teamWARReport_noEntries_returnsZeroHours() throws Exception {
        mockMvc.perform(get("/api/v1/reports/war/team/" + team.getId() + "?week=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].activityCount").value(0))
                .andExpect(jsonPath("$.data[0].plannedHours").value(0));
    }

    @Test
    void teamWARReport_withEntries_returnsSummedHours() throws Exception {
        saveWAREntry(student1, 1, 4.0, 3.5);
        saveWAREntry(student1, 1, 2.0, 2.0);

        mockMvc.perform(get("/api/v1/reports/war/team/" + team.getId() + "?week=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.firstName=='Alice')].activityCount", contains(2)))
                .andExpect(jsonPath("$.data[?(@.firstName=='Alice')].plannedHours", contains(6.0)))
                .andExpect(jsonPath("$.data[?(@.firstName=='Alice')].actualHours", contains(5.5)));
    }

    @Test
    void teamWARReport_teamNotFound_returns500() throws Exception {
        mockMvc.perform(get("/api/v1/reports/war/team/9999?week=1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── UC-33: GET /api/v1/reports/peer-eval/student/{id} ───────────────────

    @Test
    void studentPeerEvalDetail_noData_returnsEmptyWeeks() throws Exception {
        mockMvc.perform(get("/api/v1/reports/peer-eval/student/" + student1.getId() + "?startWeek=1&endWeek=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(student1.getId()))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.weeks", hasSize(2)))
                .andExpect(jsonPath("$.data.weeks[0].evaluations", hasSize(0)));
    }

    @Test
    void studentPeerEvalDetail_withData_includesEvaluatorNameAndPrivateComment() throws Exception {
        savePeerEval(student2, student1, 1, 9);

        mockMvc.perform(get("/api/v1/reports/peer-eval/student/" + student1.getId() + "?startWeek=1&endWeek=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].evaluations", hasSize(1)))
                .andExpect(jsonPath("$.data.weeks[0].evaluations[0].evaluatorName").value("Bob Report"))
                .andExpect(jsonPath("$.data.weeks[0].evaluations[0].privateComment").value("Private note"))
                .andExpect(jsonPath("$.data.weeks[0].evaluations[0].totalScore").value(9));
    }

    // ── UC-34: GET /api/v1/reports/war/student/{id} ──────────────────────────

    @Test
    void studentWARDetail_noEntries_returnsEmptyWeeks() throws Exception {
        mockMvc.perform(get("/api/v1/reports/war/student/" + student1.getId() + "?startWeek=1&endWeek=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(student1.getId()))
                .andExpect(jsonPath("$.data.weeks", hasSize(2)))
                .andExpect(jsonPath("$.data.weeks[0].activities", hasSize(0)))
                .andExpect(jsonPath("$.data.weeks[0].totalPlanned").value(0));
    }

    @Test
    void studentWARDetail_withEntries_groupedByWeek() throws Exception {
        saveWAREntry(student1, 1, 3.0, 2.5);
        saveWAREntry(student1, 1, 1.0, 1.0);
        saveWAREntry(student1, 2, 5.0, 6.0);

        mockMvc.perform(get("/api/v1/reports/war/student/" + student1.getId() + "?startWeek=1&endWeek=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].week").value(1))
                .andExpect(jsonPath("$.data.weeks[0].activities", hasSize(2)))
                .andExpect(jsonPath("$.data.weeks[0].totalPlanned").value(4.0))
                .andExpect(jsonPath("$.data.weeks[1].week").value(2))
                .andExpect(jsonPath("$.data.weeks[1].activities", hasSize(1)))
                .andExpect(jsonPath("$.data.weeks[1].totalPlanned").value(5.0));
    }
}
