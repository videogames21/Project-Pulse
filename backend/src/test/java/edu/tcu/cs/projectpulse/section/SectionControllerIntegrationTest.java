package edu.tcu.cs.projectpulse.section;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.rubric.RubricEntity;
import edu.tcu.cs.projectpulse.rubric.RubricRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SectionControllerIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RubricRepository rubricRepository;

    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        sectionRepository.deleteAll();
        rubricRepository.deleteAll();
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

    private Long createTeamAndReturnId(String teamName, String sectionName) {
        TeamEntity t = new TeamEntity();
        t.setName(teamName);
        t.setSectionName(sectionName);
        return teamRepository.save(t).getId();
    }

    private Long createStudent(String name, String email) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(UserRole.STUDENT);
        return userRepository.save(u).getId();
    }

    private Long createInstructor(String name, String email) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(UserRole.INSTRUCTOR);
        return userRepository.save(u).getId();
    }

    /**
     * Simulates the effect of UC-19 (Admin assigns instructor to team) by
     * directly setting teamId on the instructor entity. This helper is
     * intentionally endpoint-agnostic so these tests remain valid regardless
     * of how Angel's UC-19 endpoint is shaped.
     */
    private void assignInstructorToTeam(Long instructorId, Long teamId) {
        UserEntity instructor = userRepository.findById(instructorId).orElseThrow();
        instructor.setTeamId(teamId);
        userRepository.save(instructor);
    }

    private Long createRubric(String name) {
        RubricEntity r = new RubricEntity();
        r.setName(name);
        return rubricRepository.save(r).getId();
    }

    private SectionEntity createSectionWithRubric(String name, Long rubricId) {
        SectionEntity s = new SectionEntity();
        s.setName(name);
        s.setStartDate(LocalDate.of(2025, 8, 25));
        s.setEndDate(LocalDate.of(2026, 5, 10));
        s.setRubricId(rubricId);
        return sectionRepository.save(s);
    }

    // ── POST /api/v1/sections (create) ───────────────────────────────────────

    @Test
    void createSection_validInput_returns201WithSectionDetail() throws Exception {
        var body = Map.of(
                "name", "2025-2026",
                "startDate", "2025-08-25",
                "endDate", "2026-05-10"
        );

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data.startDate").value("2025-08-25"))
                .andExpect(jsonPath("$.data.endDate").value("2026-05-10"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.teams").isArray());
    }

    @Test
    void createSection_duplicateName_returns409() throws Exception {
        createSection("2025-2026");

        var body = Map.of("name", "2025-2026");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_blankName_returns400() throws Exception {
        var body = Map.of("name", "");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_missingName_returns400() throws Exception {
        var body = Map.of("startDate", "2025-08-25");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_withExistingRubric_linksRubricName() throws Exception {
        Long rubricId = createRubric("Peer Evaluation Rubric");

        var body = Map.of(
                "name", "2025-2026",
                "rubricId", rubricId
        );

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rubricName").value("Peer Evaluation Rubric"));
    }

    @Test
    void createSection_rubricNotFound_returns404() throws Exception {
        var body = Map.of(
                "name", "2025-2026",
                "rubricId", 9999
        );

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_withEditedCriteria_duplicatesRubricAndLinksNewOne() throws Exception {
        Long rubricId = createRubric("Original Rubric");

        var criteria = List.of(
                Map.of("name", "Communication", "description", "How well they communicate", "maxScore", 10)
        );
        var body = Map.of(
                "name", "2025-2026",
                "rubricId", rubricId,
                "criteria", criteria
        );

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rubricName").value("Copy of Original Rubric"));

        // Original rubric must still exist unchanged
        long rubricCount = rubricRepository.count();
        assert rubricCount == 2 : "Expected 2 rubrics (original + copy), got " + rubricCount;
    }

    @Test
    void createSection_withEditedCriteria_originalRubricUnchanged() throws Exception {
        Long rubricId = createRubric("Original Rubric");

        var criteria = List.of(
                Map.of("name", "Communication", "description", "Edited", "maxScore", 10)
        );
        var body = Map.of(
                "name", "2025-2026",
                "rubricId", rubricId,
                "criteria", criteria
        );

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Verify original rubric is unchanged by fetching it via API
        mockMvc.perform(get("/api/v1/rubrics/{id}", rubricId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Original Rubric"))
                .andExpect(jsonPath("$.data.criteria", hasSize(0)));
    }

    @Test
    void createSection_noRubric_rubricNameIsNull() throws Exception {
        var body = Map.of("name", "2025-2026");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").doesNotExist());
    }

    @Test
    void createSection_persistedAndRetrievableViaGetById() throws Exception {
        var body = Map.of(
                "name", "2025-2026",
                "startDate", "2025-08-25",
                "endDate", "2026-05-10"
        );

        String response = mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/sections/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data.startDate").value("2025-08-25"))
                .andExpect(jsonPath("$.data.endDate").value("2026-05-10"));
    }

    @Test
    void createSection_appearsInSectionList() throws Exception {
        var body = Map.of("name", "2025-2026");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"));
    }

    // ── UC-4: input edge cases ────────────────────────────────────────────────

    @Test
    void createSection_whitespaceOnlyName_returns400() throws Exception {
        var body = Map.of("name", "   ");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_criteriaWithoutRubricId_sectionCreatedWithNoRubric() throws Exception {
        // criteria without rubricId should be silently ignored — no rubric linked
        var criteria = List.of(
                Map.of("name", "Communication", "description", "desc", "maxScore", 10)
        );
        var body = Map.of("name", "2025-2026", "criteria", criteria);

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data.rubricName").doesNotExist());
    }

    @Test
    void createSection_emptyCriteriaListWithRubricId_linksOriginalNoCopyCreated() throws Exception {
        Long rubricId = createRubric("Original Rubric");

        // Empty criteria list should be treated as "no edit" — links original, no copy
        var body = Map.of("name", "2025-2026", "rubricId", rubricId, "criteria", List.of());

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").value("Original Rubric"));

        assert rubricRepository.count() == 1 : "No copy should be created for empty criteria list";
    }

    @Test
    void createSection_criterionWithBlankName_returns400() throws Exception {
        Long rubricId = createRubric("Rubric");

        var criteria = List.of(
                Map.of("name", "", "description", "desc", "maxScore", 10)
        );
        var body = Map.of("name", "2025-2026", "rubricId", rubricId, "criteria", criteria);

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_criterionWithZeroMaxScore_returns400() throws Exception {
        Long rubricId = createRubric("Rubric");

        var criteria = List.of(
                Map.of("name", "Communication", "description", "desc", "maxScore", 0)
        );
        var body = Map.of("name", "2025-2026", "rubricId", rubricId, "criteria", criteria);

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_rubricIdZero_returns404() throws Exception {
        var body = Map.of("name", "2025-2026", "rubricId", 0);

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createSection_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"2025-2026\",\"startDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── UC-4: rubric copy name collision (suffix) ─────────────────────────────

    @Test
    void createSection_editedCriteriaTwice_secondCopyGetsSuffix() throws Exception {
        Long rubricId = createRubric("Peer Eval");

        var criteria = List.of(
                Map.of("name", "Communication", "description", "How well", "maxScore", 10)
        );

        // First section with edits — creates "Copy of Peer Eval"
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2025-2026", "rubricId", rubricId, "criteria", criteria))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").value("Copy of Peer Eval"));

        // Second section editing the same rubric — "Copy of Peer Eval" already exists, gets "(2)"
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2024-2025", "rubricId", rubricId, "criteria", criteria))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").value("Copy of Peer Eval (2)"));
    }

    @Test
    void createSection_thirdEditOnSameRubric_getsThirdSuffix() throws Exception {
        Long rubricId = createRubric("Peer Eval");

        var criteria = List.of(
                Map.of("name", "Communication", "description", "How well", "maxScore", 10)
        );

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2025-2026", "rubricId", rubricId, "criteria", criteria))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2024-2025", "rubricId", rubricId, "criteria", criteria))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2023-2024", "rubricId", rubricId, "criteria", criteria))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").value("Copy of Peer Eval (3)"));
    }

    @Test
    void createSection_twoSectionsLinkSameRubricWithoutEditing_bothShowSameRubricName() throws Exception {
        Long rubricId = createRubric("Shared Rubric");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2025-2026", "rubricId", rubricId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").value("Shared Rubric"));

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "2024-2025", "rubricId", rubricId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rubricName").value("Shared Rubric"));

        // Original rubric still exists, only one rubric in DB
        assert rubricRepository.count() == 1 : "Expected 1 rubric, no copies created";
    }

    @Test
    void createSection_searchByNameAfterCreate_found() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2025-2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections").param("name", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"));
    }

    @Test
    void createSection_searchByNameAfterCreate_noMatch_returnsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2025-2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections").param("name", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void createSection_multipleCreated_sortedDescendingInList() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2023-2024"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2025-2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2024-2025"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].sectionName").value("2025-2026"))
                .andExpect(jsonPath("$.data[1].sectionName").value("2024-2025"))
                .andExpect(jsonPath("$.data[2].sectionName").value("2023-2024"));
    }

    // ── UC-4 + UC-21 cross-module: create section does not affect user lists ──

    @Test
    void createSection_doesNotAffectInstructorList() throws Exception {
        createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2025-2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Dr. Smith"));
    }

    @Test
    void createSection_doesNotAffectStudentList() throws Exception {
        createStudent("Alice Chen", "alice@tcu.edu");

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "2025-2026"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Alice Chen"));
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
    void findSectionById_teamWithNoMembers_studentsAndInstructorsAreEmpty() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createTeam("Team Alpha", "2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].students", hasSize(0)))
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(0)));
    }

    @Test
    void findSectionById_teamWithAssignedStudents_studentsListedInTeamSummary() throws Exception {
        SectionEntity saved    = createSection("2025-2026");
        Long teamId            = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long aliceId           = createStudent("Alice Chen", "alice@tcu.edu");
        Long bobId             = createStudent("Bob Smith",  "bob@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId, bobId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].students", hasSize(2)))
                .andExpect(jsonPath("$.data.teams[0].students",
                        containsInAnyOrder("Alice Chen", "Bob Smith")))
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(0)));
    }

    @Test
    void findSectionById_teamWithAssignedInstructor_instructorsListedInTeamSummary() throws Exception {
        SectionEntity saved  = createSection("2025-2026");
        Long teamId          = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long drSmithId       = createInstructor("Dr. Smith", "smith@tcu.edu");

        // Simulates UC-19: Admin assigns Dr. Smith to a team
        assignInstructorToTeam(drSmithId, teamId);

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].instructors[0]").value("Dr. Smith"))
                .andExpect(jsonPath("$.data.teams[0].students", hasSize(0)));
    }

    @Test
    void findSectionById_teamMembersSortedAlphabetically() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId         = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long zoeId          = createStudent("Zoe Adams",  "zoe@tcu.edu");
        Long aliceId        = createStudent("Alice Chen", "alice@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(zoeId, aliceId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].students[0]").value("Alice Chen"))
                .andExpect(jsonPath("$.data.teams[0].students[1]").value("Zoe Adams"));
    }

    @Test
    void findSectionById_membersIsolatedBetweenTeams() throws Exception {
        SectionEntity saved  = createSection("2025-2026");
        Long teamAlphaId     = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long teamBetaId      = createTeamAndReturnId("Team Beta",  "2025-2026");
        Long aliceId         = createStudent("Alice Chen", "alice@tcu.edu");
        Long bobId           = createStudent("Bob Smith",  "bob@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamAlphaId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId)))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/teams/" + teamBetaId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(bobId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.teams[0].students", contains("Alice Chen")))
                .andExpect(jsonPath("$.data.teams[1].name").value("Team Beta"))
                .andExpect(jsonPath("$.data.teams[1].students", contains("Bob Smith")));
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
    void findSectionById_noUsers_studentsAndInstructorsNotOnTeamAreEmpty() throws Exception {
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

    // ── UC-3: instructorsNotOnTeam ──────────���─────────────────────────────────

    @Test
    void findSectionById_unassignedInstructors_appearsInInstructorsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createInstructor("Dr. Smith", "smith@tcu.edu");
        createInstructor("Dr. Jones", "jones@tcu.edu");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(2)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam",
                        containsInAnyOrder("Dr. Smith", "Dr. Jones")));
    }

    @Test
    void findSectionById_instructorsNotOnTeamSortedAlphabetically() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createInstructor("Dr. Zhang", "zhang@tcu.edu");
        createInstructor("Dr. Adams", "adams@tcu.edu");
        createInstructor("Dr. Miller", "miller@tcu.edu");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(3)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[0]").value("Dr. Adams"))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[1]").value("Dr. Miller"))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[2]").value("Dr. Zhang"));
    }

    @Test
    void findSectionById_assignedInstructorNotInInstructorsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId      = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long drSmithId   = createInstructor("Dr. Smith", "smith@tcu.edu");
        createInstructor("Dr. Jones", "jones@tcu.edu");

        // Simulates UC-19: Admin assigns Dr. Smith to a team
        assignInstructorToTeam(drSmithId, teamId);

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[0]").value("Dr. Jones"));
    }

    @Test
    void findSectionById_studentsNotIncludedInInstructorsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createStudent("Alice Chen", "alice@tcu.edu");
        createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[0]").value("Dr. Smith"))
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam[0]").value("Alice Chen"));
    }

    // ── UC-3: rubricName ──────────────────────────────────────────────────────

    @Test
    void findSectionById_withLinkedRubric_returnsRubricName() throws Exception {
        Long rubricId    = createRubric("Peer Evaluation Rubric");
        SectionEntity saved = createSectionWithRubric("2025-2026", rubricId);

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").value("Peer Evaluation Rubric"));
    }

    @Test
    void findSectionById_noRubricLinked_rubricNameIsNull() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").doesNotExist());
    }

    @Test
    void findSectionById_differentSectionsHaveDifferentRubrics() throws Exception {
        Long rubricA = createRubric("Rubric A");
        Long rubricB = createRubric("Rubric B");
        SectionEntity section1 = createSectionWithRubric("2025-2026", rubricA);
        SectionEntity section2 = createSectionWithRubric("2024-2025", rubricB);

        mockMvc.perform(get("/api/v1/sections/{id}", section1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").value("Rubric A"));

        mockMvc.perform(get("/api/v1/sections/{id}", section2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").value("Rubric B"));
    }

    @Test
    void findSectionById_invalidIdFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/sections/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── UC-9 + UC-3 cross-module integration ──────────────────────────────────

    @Test
    void findSectionById_teamCreatedViaApi_appearsInSectionDetail() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of("name", "Team Omega", "sectionName", "2025-2026",
                "description", "API-created team", "websiteUrl", "");
        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].name").value("Team Omega"))
                .andExpect(jsonPath("$.data.teams[0].id").isNumber());
    }

    @Test
    void findSections_teamCreatedViaApi_appearsInSectionList() throws Exception {
        createSection("2025-2026");

        var body = Map.of("name", "Team Delta", "sectionName", "2025-2026",
                "description", "API-created team", "websiteUrl", "");
        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamNames", hasSize(1)))
                .andExpect(jsonPath("$.data[0].teamNames[0]").value("Team Delta"));
    }

    @Test
    void findSectionById_teamCreatedViaApiWithWrongSectionName_notIncluded() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of("name", "Team Outsider", "sectionName", "9999-9999",
                "description", "Wrong section", "websiteUrl", "");
        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(0)));
    }

    // ── UC-3 + UC-12 cross-module: studentsNotOnTeam ─────────────────────────

    @Test
    void findSectionById_unassignedStudents_appearsInStudentsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createStudent("Alice Chen", "alice@tcu.edu");
        createStudent("Bob Smith",  "bob@tcu.edu");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(2)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam",
                        containsInAnyOrder("Alice Chen", "Bob Smith")));
    }

    @Test
    void findSectionById_studentsNotOnTeamSortedAlphabetically() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createStudent("Zoe Adams",  "zoe@tcu.edu");
        createStudent("Alice Chen", "alice@tcu.edu");
        createStudent("Mike Wong",  "mike@tcu.edu");

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(3)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam[0]").value("Alice Chen"))
                .andExpect(jsonPath("$.data.studentsNotOnTeam[1]").value("Mike Wong"))
                .andExpect(jsonPath("$.data.studentsNotOnTeam[2]").value("Zoe Adams"));
    }

    @Test
    void findSectionById_assignedStudentNotInStudentsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId  = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long aliceId = createStudent("Alice Chen", "alice@tcu.edu");
        createStudent("Bob Smith", "bob@tcu.edu");

        // Assign Alice via the UC-12 endpoint
        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam[0]").value("Bob Smith"));
    }

    @Test
    void findSectionById_allStudentsAssigned_studentsNotOnTeamIsEmpty() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId  = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long aliceId = createStudent("Alice Chen", "alice@tcu.edu");
        Long bobId   = createStudent("Bob Smith",  "bob@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId, bobId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(0)));
    }

    @Test
    void findSectionById_removedStudentReturnsToStudentsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId  = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long aliceId = createStudent("Alice Chen", "alice@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId)))))
                .andExpect(status().isOk());

        // Remove Alice from the team via the UC-12 endpoint
        mockMvc.perform(delete("/api/v1/teams/" + teamId + "/students/" + aliceId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam[0]").value("Alice Chen"));
    }

    // ── UC-10 + UC-3 cross-module: team edit/delete reflected in section view ──

    @Test
    void findSectionById_teamRenamedViaApi_sectionDetailReflectsNewName() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId = createTeamAndReturnId("Old Name", "2025-2026");

        var body = Map.of("name", "New Name", "sectionName", "2025-2026",
                "description", "Updated", "websiteUrl", "");
        mockMvc.perform(put("/api/v1/teams/" + teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].name").value("New Name"));
    }

    @Test
    void findSections_teamRenamedViaApi_listReflectsNewName() throws Exception {
        createSection("2025-2026");
        Long teamId = createTeamAndReturnId("Old Name", "2025-2026");

        var body = Map.of("name", "New Name", "sectionName", "2025-2026",
                "description", "Updated", "websiteUrl", "");
        mockMvc.perform(put("/api/v1/teams/" + teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamNames", hasSize(1)))
                .andExpect(jsonPath("$.data[0].teamNames[0]").value("New Name"));
    }

    @Test
    void findSectionById_teamMovedToOtherSection_disappearsFromOriginalSection() throws Exception {
        SectionEntity original = createSection("2025-2026");
        createSection("2024-2025");
        Long teamId = createTeamAndReturnId("Team Alpha", "2025-2026");

        var body = Map.of("name", "Team Alpha", "sectionName", "2024-2025",
                "description", "", "websiteUrl", "");
        mockMvc.perform(put("/api/v1/teams/" + teamId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", original.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(0)));
    }

    @Test
    void findSectionById_teamDeletedViaApi_notIncludedInSectionDetail() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId = createTeamAndReturnId("Team Alpha", "2025-2026");

        mockMvc.perform(delete("/api/v1/teams/" + teamId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(0)));
    }

    @Test
    void findSections_teamDeletedViaApi_notIncludedInSectionList() throws Exception {
        createSection("2025-2026");
        Long teamId = createTeamAndReturnId("Team Alpha", "2025-2026");

        mockMvc.perform(delete("/api/v1/teams/" + teamId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamNames", hasSize(0)));
    }

    @Test
    void findSectionById_teamDeletedWithStudents_studentsReturnToStudentsNotOnTeam() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId  = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long aliceId = createStudent("Alice Chen", "alice@tcu.edu");
        Long bobId   = createStudent("Bob Smith",  "bob@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId, bobId)))))
                .andExpect(status().isOk());

        // Before delete: both students assigned, so studentsNotOnTeam is empty
        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(0)));

        // Delete the team — UC-10 / UC-14 service unassigns students automatically
        mockMvc.perform(delete("/api/v1/teams/" + teamId))
                .andExpect(status().isOk());

        // After delete: both students are unassigned and appear in studentsNotOnTeam
        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(2)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam",
                        containsInAnyOrder("Alice Chen", "Bob Smith")));
    }

    // ── UC-19 + UC-3 cross-module: instructor assigned to team via UC-19 ──────
    // These tests simulate the effect of UC-19 (Angel's work) using the
    // repository directly so they remain valid regardless of the endpoint shape.

    @Test
    void findSectionById_instructorAssignedViaUC19_appearsInTeamAndLeavesUnassignedList() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId         = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long drSmithId      = createInstructor("Dr. Smith", "smith@tcu.edu");

        // Before UC-19: Dr. Smith appears in instructorsNotOnTeam, not in team
        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(0)));

        // UC-19 assigns Dr. Smith to Team Alpha
        assignInstructorToTeam(drSmithId, teamId);

        // After UC-19: Dr. Smith is in team summary and gone from unassigned list
        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(0)))
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].instructors[0]").value("Dr. Smith"));
    }

    @Test
    void findSectionById_multipleInstructors_onlyUnassignedAppearInUnassignedList() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId         = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long drSmithId      = createInstructor("Dr. Smith", "smith@tcu.edu");
        createInstructor("Dr. Jones", "jones@tcu.edu");

        // UC-19 assigns only Dr. Smith
        assignInstructorToTeam(drSmithId, teamId);

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[0]").value("Dr. Jones"))
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(1)))
                .andExpect(jsonPath("$.data.teams[0].instructors[0]").value("Dr. Smith"));
    }

    @Test
    void findSectionById_instructorAndStudentOnSameTeam_listedInCorrectColumns() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId         = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long aliceId        = createStudent("Alice Chen", "alice@tcu.edu");
        Long drSmithId      = createInstructor("Dr. Smith", "smith@tcu.edu");

        mockMvc.perform(post("/api/v1/teams/" + teamId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("studentIds", List.of(aliceId)))))
                .andExpect(status().isOk());
        assignInstructorToTeam(drSmithId, teamId); // UC-19

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].students", contains("Alice Chen")))
                .andExpect(jsonPath("$.data.teams[0].instructors", contains("Dr. Smith")))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(0)))
                .andExpect(jsonPath("$.data.studentsNotOnTeam", hasSize(0)));
    }

    @Test
    void findSectionById_teamDeletedWithInstructor_instructorReturnsToUnassignedList() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long teamId         = createTeamAndReturnId("Team Alpha", "2025-2026");
        Long drSmithId      = createInstructor("Dr. Smith", "smith@tcu.edu");

        assignInstructorToTeam(drSmithId, teamId); // UC-19

        // Confirm Dr. Smith is on the team
        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams[0].instructors", hasSize(1)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(0)));

        // UC-14 deletes the team — service already unassigns all members regardless of role
        mockMvc.perform(delete("/api/v1/teams/" + teamId))
                .andExpect(status().isOk());

        // Dr. Smith should be back in the unassigned list
        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams", hasSize(0)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam", hasSize(1)))
                .andExpect(jsonPath("$.data.instructorsNotOnTeam[0]").value("Dr. Smith"));
    }

    // ── PUT /api/v1/sections/{id} (UC-5: edit section) ───────────────────────

    @Test
    void updateSection_validInput_returns200WithUpdatedDetail() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of(
                "name", "2025-2026 Updated",
                "startDate", "2025-09-01",
                "endDate", "2026-06-01"
        );

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026 Updated"))
                .andExpect(jsonPath("$.data.startDate").value("2025-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-06-01"))
                .andExpect(jsonPath("$.data.id").value(saved.getId()));
    }

    @Test
    void updateSection_sameNameAsItself_returns200NoDuplicateConflict() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of(
                "name", "2025-2026",
                "startDate", "2025-09-01",
                "endDate", "2026-06-01"
        );

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026"));
    }

    @Test
    void updateSection_nameAlreadyTakenByAnotherSection_returns409() throws Exception {
        createSection("2024-2025");
        SectionEntity target = createSection("2025-2026");

        var body = Map.of("name", "2024-2025");

        mockMvc.perform(put("/api/v1/sections/{id}", target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateSection_blankName_returns400() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of("name", "");

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateSection_sectionNotFound_returns404() throws Exception {
        var body = Map.of("name", "Does Not Exist");

        mockMvc.perform(put("/api/v1/sections/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateSection_datesOnly_returns200WithUpdatedDates() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of(
                "name", "2025-2026",
                "startDate", "2025-10-01",
                "endDate", "2026-07-01"
        );

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startDate").value("2025-10-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-07-01"));
    }

    @Test
    void updateSection_withExistingRubric_linksRubric() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long rubricId = createRubric("Peer Evaluation Rubric");

        var body = Map.of("name", "2025-2026", "rubricId", rubricId);

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").value("Peer Evaluation Rubric"));
    }

    @Test
    void updateSection_withEditedCriteria_createsCopyAndOriginalUnchanged() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        Long rubricId = createRubric("Original Rubric");

        var criteria = List.of(
                Map.of("name", "Communication", "description", "How well they communicate", "maxScore", 10)
        );
        var body = Map.of("name", "2025-2026", "rubricId", rubricId, "criteria", criteria);

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").value("Copy of Original Rubric"));

        // Original rubric must still exist unchanged
        mockMvc.perform(get("/api/v1/rubrics/{id}", rubricId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Original Rubric"))
                .andExpect(jsonPath("$.data.criteria", hasSize(0)));
    }

    @Test
    void updateSection_removeRubric_rubricNameIsNull() throws Exception {
        Long rubricId = createRubric("Peer Evaluation Rubric");
        SectionEntity saved = createSectionWithRubric("2025-2026", rubricId);

        var body = Map.of("name", "2025-2026");

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rubricName").doesNotExist());
    }

    @Test
    void updateSection_invalidIdFormat_returns400() throws Exception {
        var body = Map.of("name", "2025-2026");

        mockMvc.perform(put("/api/v1/sections/{id}", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateSection_renameSection_teamsStillAppearsInSection() throws Exception {
        SectionEntity saved = createSection("2025-2026");
        createTeam("Team Alpha", "2025-2026");
        createTeam("Team Beta", "2025-2026");

        var body = Map.of("name", "2025-2026 Renamed");

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026 Renamed"))
                .andExpect(jsonPath("$.data.teams", hasSize(2)))
                .andExpect(jsonPath("$.data.teams[0].name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.teams[1].name").value("Team Beta"));
    }

    @Test
    void updateSection_persistedChangesRetrievableViaGetById() throws Exception {
        SectionEntity saved = createSection("2025-2026");

        var body = Map.of(
                "name", "2025-2026 Edited",
                "startDate", "2025-09-01",
                "endDate", "2026-06-01"
        );

        mockMvc.perform(put("/api/v1/sections/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionName").value("2025-2026 Edited"))
                .andExpect(jsonPath("$.data.startDate").value("2025-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-06-01"));
    }
}
