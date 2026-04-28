package edu.tcu.cs.projectpulse.system;

import edu.tcu.cs.projectpulse.TestJwtHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class InstructorSectionSeederIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired TestJwtHelper jwtHelper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void seeder_allInstructorsHaveSections() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("role", "INSTRUCTOR")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data[*].supervisedSectionName",
                        everyItem(notNullValue())));
    }

    @Test
    void seeder_cs4910HasTwoInstructors_cs4911HasOne() throws Exception {
        mockMvc.perform(get("/api/v1/sections")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.sectionName == 'CS4910')].instructorIds",
                        hasItem(hasSize(2))))
                .andExpect(jsonPath("$.data[?(@.sectionName == 'CS4911')].instructorIds",
                        hasItem(hasSize(1))));
    }
}
