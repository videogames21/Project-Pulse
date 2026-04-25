package edu.tcu.cs.projectpulse.war;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class WARControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired WARRepository warRepository;
    @Autowired WARActivityRepository activityRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    // A past Monday — always valid for adding activities
    private static final String PAST_MONDAY = "2024-09-02";
    // A future Monday — invalid for adding
    private static final String FUTURE_MONDAY = "2099-01-06";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        warRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long createStudent() {
        UserEntity u = new UserEntity();
        u.setFirstName("Alice");
        u.setLastName("Chen");
        u.setEmail("alice@tcu.edu");
        u.setRole(UserRole.STUDENT);
        return userRepository.save(u).getId();
    }

    private Map<String, Object> validActivityBody() {
        return Map.of(
                "category", "DEVELOPMENT",
                "description", "Fix the login bug",
                "plannedHours", new BigDecimal("4.0"),
                "actualHours", new BigDecimal("5.0"),
                "status", "DONE"
        );
    }

    private Long addActivity(Long studentId, String weekStart) throws Exception {
        String response = mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + weekStart + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validActivityBody())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").longValue();
    }

    // ── GET WAR ──────────────────────────────────────────────────────────────

    @Test
    void getWAR_noActivities_returnsEmptyWAR() throws Exception {
        Long studentId = createStudent();

        mockMvc.perform(get("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentId))
                .andExpect(jsonPath("$.data.weekStart").value(PAST_MONDAY))
                .andExpect(jsonPath("$.data.activities", hasSize(0)));
    }

    @Test
    void getWAR_studentNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/wars/students/9999/weeks/" + PAST_MONDAY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getWAR_weekStartNotMonday_returns400() throws Exception {
        Long studentId = createStudent();

        // 2024-09-03 is a Tuesday
        mockMvc.perform(get("/api/v1/wars/students/" + studentId + "/weeks/2024-09-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST activity ────────────────────────────────────────────────────────

    @Test
    void addActivity_validRequest_returns201AndPersists() throws Exception {
        Long studentId = createStudent();

        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validActivityBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.data.description").value("Fix the login bug"))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    void addActivity_activitiesAppearInGetWAR() throws Exception {
        Long studentId = createStudent();
        addActivity(studentId, PAST_MONDAY);
        addActivity(studentId, PAST_MONDAY);

        mockMvc.perform(get("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activities", hasSize(2)));
    }

    @Test
    void addActivity_futureWeek_returns400() throws Exception {
        Long studentId = createStudent();

        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + FUTURE_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validActivityBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addActivity_studentNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/wars/students/9999/weeks/" + PAST_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validActivityBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addActivity_missingCategory_returns400() throws Exception {
        Long studentId = createStudent();
        var body = Map.of("description", "desc", "plannedHours", "4.0", "status", "DONE");

        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addActivity_blankDescription_returns400() throws Exception {
        Long studentId = createStudent();
        var body = Map.of("category", "DEVELOPMENT", "description", "", "plannedHours", "4.0", "status", "DONE");

        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addActivity_missingActualHours_returns400() throws Exception {
        Long studentId = createStudent();
        var body = Map.of("category", "DEVELOPMENT", "description", "desc",
                "plannedHours", new BigDecimal("4.0"), "status", "DONE");

        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addActivity_negativePlannedHours_returns400() throws Exception {
        Long studentId = createStudent();
        var body = Map.of("category", "DEVELOPMENT", "description", "desc",
                "plannedHours", new BigDecimal("-1.0"), "actualHours", new BigDecimal("1.0"), "status", "DONE");

        mockMvc.perform(post("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PUT activity ─────────────────────────────────────────────────────────

    @Test
    void updateActivity_validRequest_returns200AndUpdates() throws Exception {
        Long studentId = createStudent();
        Long activityId = addActivity(studentId, PAST_MONDAY);

        var updateBody = Map.of(
                "category", "TESTING",
                "description", "Write unit tests",
                "plannedHours", new BigDecimal("3.0"),
                "actualHours", new BigDecimal("2.0"),
                "status", "IN_PROGRESS"
        );

        mockMvc.perform(put("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities/" + activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("TESTING"))
                .andExpect(jsonPath("$.data.description").value("Write unit tests"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void updateActivity_warNotFound_returns404() throws Exception {
        Long studentId = createStudent();

        var body = Map.of("category", "TESTING", "description", "desc",
                "plannedHours", new BigDecimal("3.0"), "actualHours", new BigDecimal("2.0"), "status", "DONE");

        mockMvc.perform(put("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateActivity_activityNotFound_returns404() throws Exception {
        Long studentId = createStudent();
        addActivity(studentId, PAST_MONDAY);

        var body = Map.of("category", "TESTING", "description", "desc",
                "plannedHours", new BigDecimal("3.0"), "actualHours", new BigDecimal("2.0"), "status", "DONE");

        mockMvc.perform(put("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── DELETE activity ──────────────────────────────────────────────────────

    @Test
    void deleteActivity_validRequest_returns200AndRemoves() throws Exception {
        Long studentId = createStudent();
        Long activityId = addActivity(studentId, PAST_MONDAY);

        mockMvc.perform(delete("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities/" + activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activities", hasSize(0)));
    }

    @Test
    void deleteActivity_activityNotFound_returns404() throws Exception {
        Long studentId = createStudent();
        addActivity(studentId, PAST_MONDAY);

        mockMvc.perform(delete("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteActivity_warNotFound_returns404() throws Exception {
        Long studentId = createStudent();

        mockMvc.perform(delete("/api/v1/wars/students/" + studentId + "/weeks/" + PAST_MONDAY + "/activities/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
