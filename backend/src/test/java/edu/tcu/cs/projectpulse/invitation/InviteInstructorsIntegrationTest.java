package edu.tcu.cs.projectpulse.invitation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class InviteInstructorsIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired InvitationRepository repository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        repository.deleteAll();
    }

    // ── POST /api/v1/invitations/instructors ──────────────────────────────────

    @Test
    void inviteInstructors_singleEmail_returns201WithLink() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emails", "smith@tcu.edu"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email").value("smith@tcu.edu"))
                .andExpect(jsonPath("$.data[0].registrationLink").isString())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void inviteInstructors_multipleEmails_createsOneInvitationEach() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("emails", "smith@tcu.edu; jones@tcu.edu; lee@tcu.edu"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    void inviteInstructors_emailsWithExtraSpaces_parsedCorrectly() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("emails", "  smith@tcu.edu ;  jones@tcu.edu  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void inviteInstructors_eachLinkIsUnique() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("emails", "smith@tcu.edu; jones@tcu.edu"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var tree = objectMapper.readTree(response).path("data");
        String link1 = tree.get(0).path("registrationLink").asText();
        String link2 = tree.get(1).path("registrationLink").asText();
        assertThat(link1).isNotEqualTo(link2);
    }

    @Test
    void inviteInstructors_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emails", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void inviteInstructors_mixedValidInvalid_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("emails", "smith@tcu.edu; bad-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void inviteInstructors_emptyEmails_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("emails", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void inviteInstructors_missingEmailsField_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invitations/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
