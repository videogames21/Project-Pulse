package edu.tcu.cs.projectpulse.section;

import edu.tcu.cs.projectpulse.team.TeamEntity;
import edu.tcu.cs.projectpulse.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SectionControllerIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TeamRepository teamRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        teamRepository.deleteAll();
        sectionRepository.deleteAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SectionEntity createSection(String name) {
        SectionEntity s = new SectionEntity();
        s.setName(name);
        s.setStartDate(LocalDate.of(2025, 8, 25));
        s.setEndDate(LocalDate.of(2026, 5, 10));
        return sectionRepository.save(s);
    }

    private void createTeam(String teamName, String sectionName) {
        TeamEntity t = new TeamEntity();
        t.setName(teamName);
        t.setSectionName(sectionName);
        teamRepository.save(t);
    }

    // ── GET /api/v1/sections (no filter) ─────────────────────────────────────

    @Test
    void findSections_noSections_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findSections_noFilter_returnsAllSectionsSortedDescending() throws Exception {
        createSection("2023-2024");
        createSection("2025-2026");
        createSection("2024-2025");

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data[1].sectionName").value("2024-2025"))
                .andExpect(jsonPath("$.data[2].sectionName").value("2023-2024"));
    }

    // ── GET /api/v1/sections?name=xxx (with filter) ───────────────────────────

    @Test
    void findSections_filterByName_returnsMatchingSections() throws Exception {
        createSection("2025-2026");
        createSection("2024-2025");
        createSection("2022-2023");

        mockMvc.perform(get("/api/v1/sections").param("name", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].sectionName", containsInAnyOrder("2025-2026", "2024-2025")));
    }

    @Test
    void findSections_filterByName_caseInsensitive() throws Exception {
        createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections").param("name", "2025-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"));
    }

    @Test
    void findSections_filterByName_noMatch_returnsEmptyList() throws Exception {
        createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections").param("name", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ── Team names in response ────────────────────────────────────────────────

    @Test
    void findSections_withTeams_includesTeamNamesSortedAscending() throws Exception {
        createSection("2025-2026");
        createTeam("Team Zeta", "2025-2026");
        createTeam("Team Alpha", "2025-2026");
        createTeam("Team Beta", "2025-2026");

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data[0].teamNames", hasSize(3)))
                .andExpect(jsonPath("$.data[0].teamNames[0]").value("Team Alpha"))
                .andExpect(jsonPath("$.data[0].teamNames[1]").value("Team Beta"))
                .andExpect(jsonPath("$.data[0].teamNames[2]").value("Team Zeta"));
    }

    @Test
    void findSections_noTeams_returnsEmptyTeamNamesList() throws Exception {
        createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data[0].teamNames", hasSize(0)));
    }

    @Test
    void findSections_teamsOnlyIncludedForMatchingSection() throws Exception {
        createSection("2025-2026");
        createSection("2024-2025");
        createTeam("Team A", "2025-2026");
        createTeam("Team B", "2024-2025");

        mockMvc.perform(get("/api/v1/sections").param("name", "2025-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data[0].teamNames", contains("Team A")));
    }

    // ── GET /api/v1/sections (list enrichment) ────────────────────────────────

    @Test
    void findSections_listIncludesIdStartDateEndDate() throws Exception {
        createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].startDate").value("2025-08-25"))
                .andExpect(jsonPath("$.data[0].endDate").value("2026-05-10"));
    }

    // ── GET /api/v1/sections/{id} ─────────────────────────────────────────────

    @Test
    void findSectionById_exists_returnsDetail() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data.startDate").value("2025-08-25"))
                .andExpect(jsonPath("$.data.endDate").value("2026-05-10"));
    }

    @Test
    void findSectionById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/sections/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void findSectionById_noTeams_returnsEmptyTeamsList() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(0)));
    }

    @Test
    void findSectionById_withTeams_returnsTeamsSortedAscending() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createTeam("Team Zeta", "2025-2026");
        createTeam("Team Alpha", "2025-2026");
        createTeam("Team Beta", "2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(3)))
                .andExpect(jsonPath("$.data.teams[0].name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.teams[1].name").value("Team Beta"))
                .andExpect(jsonPath("$.data.teams[2].name").value("Team Zeta"));
    }

    @Test
    void findSectionById_teamSummaryIncludesId() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createTeam("Team Alpha", "2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].id").isNumber())
                .andExpect(jsonPath("$.data.teams[0].name").value("Team Alpha"));
    }

    @Test
    void findSectionById_teamsFromOtherSectionNotIncluded() throws Exception {
        SectionEntity target = createSection("2025-2026");
        createSection("2024-2025");
        createTeam("Team A", "2025-2026");
        createTeam("Team B", "2024-2025");

        mockMvc.perform(get("/api/v1/sections/{id}", target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].name").value("Team A"));
    }

    @Test
    void findSectionById_nullDates_returnsNullStartAndEndDate() throws Exception {
        SectionEntity s = new SectionEntity();
        s.setName("2025-2026");
        SectionEntity saved = sectionRepository.save(s);

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").doesNotExist())
                .andExpect(jsonPath("$.data.endDate").doesNotExist());
    }

    @Test
    void findSectionById_stubFieldsAreEmptyArraysNotNull() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam").isArray())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(0)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam").isArray())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(0)));
    }

    @Test
    void findSectionById_rubricNameFieldPresentAndNull() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").doesNotExist());
    }

    @Test
    void findSectionById_invalidIdFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/sections/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
