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
class SectionPeerEvaluationReportIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired PeerEvaluationRepository peerEvaluationRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    private static final String SECTION = "CS4910";
    private static final String PAST_MONDAY = "2024-09-02";

    Long teamId;
    Long aliceId;
    Long bobId;
    Long carolId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        peerEvaluationRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        TeamEntity team = new TeamEntity();
        team.setName("Team Alpha");
        team.setSectionName(SECTION);
        teamId = teamRepository.save(team).getId();

        aliceId = createStudent("Alice Johnson", "alice@tcu.edu");
        bobId   = createStudent("Bob Smith",     "bob@tcu.edu");
        carolId = createStudent("Carol White",   "carol@tcu.edu");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long createStudent(String name, String email) {
        String[] parts = name.split(" ", 2);
        UserEntity u = new UserEntity();
        u.setFirstName(parts[0]);
        u.setLastName(parts.length > 1 ? parts[1] : "");
        u.setEmail(email);
        u.setRole(UserRole.STUDENT);
        u.setTeamId(teamId);
        return userRepository.save(u).getId();
    }

    private void submitEval(Long evaluatorId, Long evaluateeId, String weekStart,
                            int score1, int score2,
                            String pub, String priv) throws Exception {
        var body = Map.of(
                "evaluatorId", evaluatorId,
                "evaluateeId", evaluateeId,
                "weekStart", weekStart,
                "scores", List.of(
                        Map.of("criterionId", 1L, "score", score1),
                        Map.of("criterionId", 2L, "score", score2)
                ),
                "publicComments", pub,
                "privateComments", priv
        );
        mockMvc.perform(post("/api/v1/peer-evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void getSectionReport_noEvaluations_returnsAllStudentsWithZeroGrade() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").value(SECTION))
                .andExpect(jsonPath("$.data.weekStart").value(PAST_MONDAY))
                .andExpect(jsonPath("$.data.students", hasSize(3)))
                .andExpect(jsonPath("$.data.students[0].evaluationCount").value(0))
                .andExpect(jsonPath("$.data.students[0].averageGrade").value(0))
                .andExpect(jsonPath("$.data.students[0].didSubmit").value(false));
    }

    @Test
    void getSectionReport_studentsSortedByLastName() throws Exception {
        // Last names: Johnson, Smith, White → alphabetical order
        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.students[1].studentName").value("Bob Smith"))
                .andExpect(jsonPath("$.data.students[2].studentName").value("Carol White"));
    }

    @Test
    void getSectionReport_gradeComputedCorrectly() throws Exception {
        // Bob gives Alice: scores 8+10=18; Carol gives Alice: scores 6+8=14 → avg=(18+14)/2=16.00
        submitEval(bobId,   aliceId, PAST_MONDAY, 8, 10, "Good.", "Note B.");
        submitEval(carolId, aliceId, PAST_MONDAY, 6, 8,  "OK.",   "Note C.");

        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.students[0].evaluationCount").value(2))
                .andExpect(jsonPath("$.data.students[0].averageGrade").value(16.00));
    }

    @Test
    void getSectionReport_includesPrivateCommentsAndEvaluatorNames() throws Exception {
        submitEval(bobId, aliceId, PAST_MONDAY, 9, 9, "Great work!", "Needs focus.");

        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].receivedEvaluations[0].evaluatorName").value("Bob Smith"))
                .andExpect(jsonPath("$.data.students[0].receivedEvaluations[0].publicComments").value("Great work!"))
                .andExpect(jsonPath("$.data.students[0].receivedEvaluations[0].privateComments").value("Needs focus."));
    }

    @Test
    void getSectionReport_didSubmitFlagCorrect() throws Exception {
        // Alice submits an eval; Bob and Carol do not
        submitEval(aliceId, bobId, PAST_MONDAY, 8, 8, "", "");

        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.students[0].didSubmit").value(true))
                .andExpect(jsonPath("$.data.students[1].studentName").value("Bob Smith"))
                .andExpect(jsonPath("$.data.students[1].didSubmit").value(false))
                .andExpect(jsonPath("$.data.students[2].studentName").value("Carol White"))
                .andExpect(jsonPath("$.data.students[2].didSubmit").value(false));
    }

    @Test
    void getSectionReport_onlyIncludesStudentsFromRequestedSection() throws Exception {
        // Add a second section with its own team and student
        TeamEntity otherTeam = new TeamEntity();
        otherTeam.setName("Team Beta");
        otherTeam.setSectionName("CS4911");
        Long otherTeamId = teamRepository.save(otherTeam).getId();

        UserEntity zara = new UserEntity();
        zara.setFirstName("Zara");
        zara.setLastName("Young");
        zara.setEmail("zara@tcu.edu");
        zara.setRole(UserRole.STUDENT);
        zara.setTeamId(otherTeamId);
        userRepository.save(zara);

        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(3)));  // Zara not included
    }

    @Test
    void getSectionReport_weekStartNotMonday_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/sections/" + SECTION + "/report")
                        .param("weekStart", "2024-09-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getSectionReport_unknownSection_returnsEmptyStudentList() throws Exception {
        mockMvc.perform(get("/api/v1/peer-evaluations/sections/UNKNOWN/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(0)));
    }
}
