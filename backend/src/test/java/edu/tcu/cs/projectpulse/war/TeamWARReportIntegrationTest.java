package edu.tcu.cs.projectpulse.war;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class TeamWARReportIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired WARRepository warRepository;
    @Autowired WARActivityRepository activityRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    private static final String PAST_MONDAY = "2024-09-02";
    private static final String ANOTHER_MONDAY = "2024-09-09";

    Long teamId;
    Long aliceId;
    Long bobId;
    Long carolId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        activityRepository.deleteAll();
        warRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        TeamEntity team = new TeamEntity();
        team.setName("Team Alpha");
        team.setSectionName("CS4910");
        teamId = teamRepository.save(team).getId();

        aliceId = createStudent("Alice", "Johnson", "alice@tcu.edu");
        bobId   = createStudent("Bob",   "Smith",   "bob@tcu.edu");
        carolId = createStudent("Carol", "White",   "carol@tcu.edu");
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

    private void addActivity(Long studentId, String weekStart) throws Exception {
        var body = Map.of(
                "category", "DEVELOPMENT",
                "description", "Fix the login bug",
                "plannedHours", "4.0",
                "actualHours", "5.0",
                "status", "DONE"
        );
        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + weekStart + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void getTeamReport_noSubmissions_allStudentsWithEmptyActivities() throws Exception {
        mockMvc.perform(get("/api/v1/wars/teams/" + teamId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamId").value(teamId))
                .andExpect(jsonPath("$.data.teamName").value("Team Alpha"))
                .andExpect(jsonPath("$.data.weekStart").value(PAST_MONDAY))
                .andExpect(jsonPath("$.data.students", hasSize(3)))
                .andExpect(jsonPath("$.data.students[0].didSubmit").value(false))
                .andExpect(jsonPath("$.data.students[0].activities", hasSize(0)));
    }

    @Test
    void getTeamReport_studentsSortedByLastName() throws Exception {
        // Last names: Johnson, Smith, White → alphabetical order
        mockMvc.perform(get("/api/v1/wars/teams/" + teamId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.students[1].studentName").value("Bob Smith"))
                .andExpect(jsonPath("$.data.students[2].studentName").value("Carol White"));
    }

    @Test
    void getTeamReport_submittedStudentShowsActivities() throws Exception {
        addActivity(aliceId, PAST_MONDAY);
        addActivity(aliceId, PAST_MONDAY);

        mockMvc.perform(get("/api/v1/wars/teams/" + teamId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.students[0].didSubmit").value(true))
                .andExpect(jsonPath("$.data.students[0].activities", hasSize(2)))
                .andExpect(jsonPath("$.data.students[0].activities[0].category").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.data.students[0].activities[0].description").value("Fix the login bug"));
    }

    @Test
    void getTeamReport_mixedSubmissions_didSubmitFlagCorrect() throws Exception {
        addActivity(aliceId, PAST_MONDAY);
        // Bob and Carol do not submit

        mockMvc.perform(get("/api/v1/wars/teams/" + teamId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].didSubmit").value(true))
                .andExpect(jsonPath("$.data.students[1].didSubmit").value(false))
                .andExpect(jsonPath("$.data.students[2].didSubmit").value(false));
    }

    @Test
    void getTeamReport_onlyShowsActivitiesForRequestedWeek() throws Exception {
        addActivity(aliceId, PAST_MONDAY);
        addActivity(aliceId, ANOTHER_MONDAY);

        mockMvc.perform(get("/api/v1/wars/teams/" + teamId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].activities", hasSize(1)));
    }

    @Test
    void getTeamReport_teamNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/wars/teams/9999/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getTeamReport_weekStartNotMonday_returns400() throws Exception {
        // 2024-09-03 is a Tuesday
        mockMvc.perform(get("/api/v1/wars/teams/" + teamId + "/report")
                        .param("weekStart", "2024-09-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getTeamReport_emptyTeam_returnsEmptyStudentList() throws Exception {
        TeamEntity emptyTeam = new TeamEntity();
        emptyTeam.setName("Empty Team");
        emptyTeam.setSectionName("CS4910");
        Long emptyTeamId = teamRepository.save(emptyTeam).getId();

        mockMvc.perform(get("/api/v1/wars/teams/" + emptyTeamId + "/report")
                        .param("weekStart", PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students", hasSize(0)));
    }
}
