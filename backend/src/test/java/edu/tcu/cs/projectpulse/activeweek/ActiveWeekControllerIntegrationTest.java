package edu.tcu.cs.projectpulse.activeweek;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.section.SectionEntity;
import edu.tcu.cs.projectpulse.section.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ActiveWeekControllerIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    ActiveWeekRepository activeWeekRepository;

    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        activeWeekRepository.deleteAll();
        sectionRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SectionEntity createSection(String name, LocalDate startDate, LocalDate endDate) {
        SectionEntity s = new SectionEntity();
        s.setName(name);
        s.setStartDate(startDate);
        s.setEndDate(endDate);
        return sectionRepository.save(s);
    }

    // ── PUT /api/v1/sections/{id}/active-weeks ────────────────────────────────

    @Test
    void saveActiveWeeks_validMondays_returns200WithSavedWeeks() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        var body = Map.of("activeWeekDates", List.of("2025-09-01", "2025-09-08", "2025-09-15"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionId").value(section.getId()))
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(3)))
                .andExpect(jsonPath("$.data.activeWeeks[0]").value("2025-09-01"))
                .andExpect(jsonPath("$.data.activeWeeks[1]").value("2025-09-08"))
                .andExpect(jsonPath("$.data.activeWeeks[2]").value("2025-09-15"));
    }

    @Test
    void saveActiveWeeks_emptyList_returns200AndClearsAllWeeks() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        // First, save some weeks
        var firstBody = Map.of("activeWeekDates", List.of("2025-09-01", "2025-09-08"));
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBody)))
                .andExpect(status().isOk());

        // Now clear them all
        var emptyBody = Map.of("activeWeekDates", List.of());
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(0)));
    }

    @Test
    void saveActiveWeeks_sectionNotFound_returns404() throws Exception {
        var body = Map.of("activeWeekDates", List.of("2025-09-01"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveActiveWeeks_nonMondayDate_returns400() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        // 2025-09-02 is a Tuesday
        var body = Map.of("activeWeekDates", List.of("2025-09-02"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveActiveWeeks_dateBeforeSectionStartDate_returns400() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 5, 10));

        // 2025-08-25 is a Monday but before startDate 2025-09-01
        var body = Map.of("activeWeekDates", List.of("2025-08-25"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveActiveWeeks_dateAfterSectionEndDate_returns400() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 4));

        // 2026-05-11 is a Monday but after endDate 2026-05-04
        var body = Map.of("activeWeekDates", List.of("2026-05-11"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveActiveWeeks_replacesExistingWeeks_secondPutWins() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        var firstBody = Map.of("activeWeekDates", List.of("2025-09-01", "2025-09-08"));
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBody)))
                .andExpect(status().isOk());

        // Second PUT with different dates — should fully replace
        var secondBody = Map.of("activeWeekDates", List.of("2025-09-15", "2025-09-22"));
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(2)))
                .andExpect(jsonPath("$.data.activeWeeks[0]").value("2025-09-15"))
                .andExpect(jsonPath("$.data.activeWeeks[1]").value("2025-09-22"));
    }

    @Test
    void saveActiveWeeks_sortedAscendingInResponse() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        // Send dates out of order
        var body = Map.of("activeWeekDates", List.of("2025-09-22", "2025-09-01", "2025-09-15"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks[0]").value("2025-09-01"))
                .andExpect(jsonPath("$.data.activeWeeks[1]").value("2025-09-15"))
                .andExpect(jsonPath("$.data.activeWeeks[2]").value("2025-09-22"));
    }

    @Test
    void saveActiveWeeks_nullActiveWeekDates_returns400() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        var body = new java.util.HashMap<String, Object>();
        body.put("activeWeekDates", null);

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/v1/sections/{id}/active-weeks ────────────────────────────────

    @Test
    void getActiveWeeks_noWeeksSaved_returnsEmptyList() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", section.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionId").value(section.getId()))
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(0)));
    }

    @Test
    void getActiveWeeks_afterSave_returnsCorrectWeeks() throws Exception {
        SectionEntity section = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));

        var body = Map.of("activeWeekDates", List.of("2025-09-01", "2025-09-08"));
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", section.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(2)))
                .andExpect(jsonPath("$.data.activeWeeks[0]").value("2025-09-01"))
                .andExpect(jsonPath("$.data.activeWeeks[1]").value("2025-09-08"));
    }

    @Test
    void getActiveWeeks_sectionNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveActiveWeeks_sectionHasNoDates_returns400() throws Exception {
        // Section with no startDate or endDate
        SectionEntity section = new SectionEntity();
        section.setName("No-Dates Section");
        section = sectionRepository.save(section);

        var body = Map.of("activeWeekDates", List.of("2025-09-01"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── deleteOutOfRange isolation (regression for OR-precedence bug) ──────────

    @Test
    void updateSectionDateRange_prunesOnlyTargetSectionWeeks() throws Exception {
        // Section A: will have its end date shortened
        SectionEntity sectionA = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));
        // Section B: should not be touched by section A's update
        SectionEntity sectionB = createSection("2024-2025",
                LocalDate.of(2024, 8, 26), LocalDate.of(2026, 12, 31));

        // Give section A a week that will fall outside after its end is shortened
        var bodyA = Map.of("activeWeekDates", List.of("2026-01-05")); // 2026-01-05 is a Monday
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", sectionA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyA)))
                .andExpect(status().isOk());

        // Give section B a week on the same date — this must survive section A's update
        var bodyB = Map.of("activeWeekDates", List.of("2026-01-05"));
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", sectionB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyB)))
                .andExpect(status().isOk());

        // Shorten section A's end date so 2026-01-05 is now out of range
        var sectionUpdate = Map.of(
                "name",      sectionA.getName(),
                "startDate", "2025-08-25",
                "endDate",   "2025-12-31"
        );
        mockMvc.perform(put("/api/v1/sections/{id}", sectionA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sectionUpdate)))
                .andExpect(status().isOk());

        // Section A's week should be pruned
        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", sectionA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(0)));

        // Section B's week must be untouched
        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", sectionB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(1)))
                .andExpect(jsonPath("$.data.activeWeeks[0]").value("2026-01-05"));
    }

    // ── configuredWeeks — expansion vs. deliberate deactivation ──────────────

    @Test
    void saveActiveWeeks_deactivatedWeek_appearsInConfiguredButNotActive() throws Exception {
        // Range has 3 Mondays: Sep 1, Sep 8, Sep 15. Deactivate Sep 8.
        SectionEntity section = createSection("2025-Fall",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 15));

        var body = Map.of("activeWeekDates", List.of("2025-09-01", "2025-09-15"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks",    hasSize(2)))
                .andExpect(jsonPath("$.data.configuredWeeks", hasSize(3)))
                .andExpect(jsonPath("$.data.configuredWeeks", hasItem("2025-09-08")));
    }

    @Test
    void getActiveWeeks_afterDeactivation_configuredWeeksRetained() throws Exception {
        SectionEntity section = createSection("2025-Fall",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 15));

        var body = Map.of("activeWeekDates", List.of("2025-09-01", "2025-09-15"));

        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", section.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", section.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks",    hasSize(2)))
                .andExpect(jsonPath("$.data.configuredWeeks", hasSize(3)));
    }

    @Test
    void getActiveWeeks_weeksIsolatedBetweenSections() throws Exception {
        SectionEntity sectionA = createSection("2025-2026",
                LocalDate.of(2025, 8, 25), LocalDate.of(2026, 5, 10));
        SectionEntity sectionB = createSection("2024-2025",
                LocalDate.of(2024, 8, 26), LocalDate.of(2025, 5, 10));

        var bodyA = Map.of("activeWeekDates", List.of("2025-09-01"));
        mockMvc.perform(put("/api/v1/sections/{id}/active-weeks", sectionA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyA)))
                .andExpect(status().isOk());

        // Section B should have no active weeks
        mockMvc.perform(get("/api/v1/sections/{id}/active-weeks", sectionB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeWeeks", hasSize(0)));
    }
}
