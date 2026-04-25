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
class StudentWARRangeReportIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired WARRepository warRepository;
    @Autowired WARActivityRepository activityRepository;
    @Autowired UserRepository userRepository;
    @Autowired TeamRepository teamRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    private static final String WEEK1 = "2024-09-02";
    private static final String WEEK2 = "2024-09-09";
    private static final String WEEK3 = "2024-09-16";

    Long teamId;
    Long aliceId;

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
    void getRangeReport_noActivities_returnsEmptyWeeksList() throws Exception {
        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(aliceId))
                .andExpect(jsonPath("$.data.studentName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.startWeek").value(WEEK1))
                .andExpect(jsonPath("$.data.endWeek").value(WEEK3))
                .andExpect(jsonPath("$.data.weeks", hasSize(0)));
    }

    @Test
    void getRangeReport_activitiesInRange_groupedByWeek() throws Exception {
        addActivity(aliceId, WEEK1);
        addActivity(aliceId, WEEK2);

        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks", hasSize(2)))
                .andExpect(jsonPath("$.data.weeks[0].weekStart").value(WEEK1))
                .andExpect(jsonPath("$.data.weeks[1].weekStart").value(WEEK2));
    }

    @Test
    void getRangeReport_multipleActivitiesInSameWeek() throws Exception {
        addActivity(aliceId, WEEK1);
        addActivity(aliceId, WEEK1);

        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks", hasSize(1)))
                .andExpect(jsonPath("$.data.weeks[0].activities", hasSize(2)));
    }

    @Test
    void getRangeReport_activityFieldsPresent() throws Exception {
        addActivity(aliceId, WEEK1);

        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].activities[0].category").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.data.weeks[0].activities[0].description").value("Fix the login bug"))
                .andExpect(jsonPath("$.data.weeks[0].activities[0].plannedHours").value(4.0))
                .andExpect(jsonPath("$.data.weeks[0].activities[0].actualHours").value(5.0))
                .andExpect(jsonPath("$.data.weeks[0].activities[0].status").value("DONE"));
    }

    @Test
    void getRangeReport_sortedChronologically() throws Exception {
        addActivity(aliceId, WEEK2);
        addActivity(aliceId, WEEK1);

        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks[0].weekStart").value(WEEK1))
                .andExpect(jsonPath("$.data.weeks[1].weekStart").value(WEEK2));
    }

    @Test
    void getRangeReport_excludesActivitiesOutsideRange() throws Exception {
        addActivity(aliceId, WEEK1);
        addActivity(aliceId, WEEK3);

        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weeks", hasSize(1)))
                .andExpect(jsonPath("$.data.weeks[0].weekStart").value(WEEK1));
    }

    @Test
    void getRangeReport_studentNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/wars/students/9999/report")
                        .param("startWeek", WEEK1)
                        .param("endWeek", WEEK2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRangeReport_startWeekNotMonday_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", "2024-09-03")
                        .param("endWeek", WEEK2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRangeReport_endBeforeStart_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/wars/students/" + aliceId + "/report")
                        .param("startWeek", WEEK3)
                        .param("endWeek", WEEK1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
