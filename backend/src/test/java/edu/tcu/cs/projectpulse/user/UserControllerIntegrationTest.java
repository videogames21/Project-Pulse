package edu.tcu.cs.projectpulse.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UserControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        userRepository.deleteAll();
    }

    private void createUser(String name, String email, UserRole role) {
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(role);
        userRepository.save(u);
    }

    // ── GET /api/v1/users?role=INSTRUCTOR ────────────────────────────────────

    @Test
    void findInstructors_noInstructors_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findInstructors_returnsOnlyInstructors() throws Exception {
        createUser("Alice Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Bob Student", "bob@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Alice Prof"))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    @Test
    void findInstructors_multipleInstructors_returnsAll() throws Exception {
        createUser("Alice Prof",   "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Charlie Prof", "charlie@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_responseIncludesIdNameEmailRole() throws Exception {
        createUser("Alice Prof", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].name").value("Alice Prof"))
                .andExpect(jsonPath("$.data[0].email").value("alice@tcu.edu"))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    // ── GET /api/v1/users?role=INSTRUCTOR&name= ───────────────────────────────

    @Test
    void findInstructors_nameFilter_returnsMatchingInstructors() throws Exception {
        createUser("Alice Smith",   "alice@tcu.edu",   UserRole.INSTRUCTOR);
        createUser("Bob Johnson",   "bob@tcu.edu",     UserRole.INSTRUCTOR);
        createUser("Alice Teacher", "alice2@tcu.edu",  UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findInstructors_nameFilter_caseInsensitive() throws Exception {
        createUser("Alice Smith", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Alice Smith"));
    }

    @Test
    void findInstructors_nameFilter_noMatch_returnsEmpty() throws Exception {
        createUser("Alice Smith", "alice@tcu.edu", UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void findInstructors_nameFilter_doesNotReturnStudentsWithSameName() throws Exception {
        createUser("Alice Smith", "alice-inst@tcu.edu",    UserRole.INSTRUCTOR);
        createUser("Alice Smith", "alice-student@tcu.edu", UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].role").value("INSTRUCTOR"));
    }

    @Test
    void findInstructors_emptyNameParam_returnsAllInstructors() throws Exception {
        createUser("Alice Prof",   "alice@tcu.edu",   UserRole.INSTRUCTOR);
        createUser("Bob Prof",     "bob@tcu.edu",     UserRole.INSTRUCTOR);

        mockMvc.perform(get("/api/v1/users").param("role", "INSTRUCTOR").param("name", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // ── Existing student behaviour not broken ─────────────────────────────────

    @Test
    void findStudents_roleStudentParam_returnsStudentsOnly() throws Exception {
        createUser("Alice Prof",    "alice@tcu.edu", UserRole.INSTRUCTOR);
        createUser("Bob Student",   "bob@tcu.edu",   UserRole.STUDENT);

        mockMvc.perform(get("/api/v1/users").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Bob Student"));
    }
}
