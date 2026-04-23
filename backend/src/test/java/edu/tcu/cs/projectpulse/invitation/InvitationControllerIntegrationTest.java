package edu.tcu.cs.projectpulse.invitation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class InvitationControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired InvitationRepository invitationRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        invitationRepository.deleteAll();
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private String createInvitation() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations"))
                .andReturn().getResponse().getContentAsString();
        String link = objectMapper.readTree(response).path("data").path("registrationLink").asText();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    // ── POST /api/v1/invitations ─────────────────────────────────────────────

    @Test
    void generateInvitation_success_returns200WithLink() throws Exception {
        mockMvc.perform(post("/api/v1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationLink", startsWith("http://localhost:3000/register/")))
                .andExpect(jsonPath("$.data.tokenShort", hasLength(8)))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.invitedBy").value("admin@tcu.edu"));
    }

    @Test
    void generateInvitation_twoCalls_produceDifferentTokens() throws Exception {
        String token1 = createInvitation();
        String token2 = createInvitation();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateInvitation_persistsToDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/invitations"))
                .andExpect(status().isOk());

        assertThat(invitationRepository.count()).isEqualTo(1);
    }

    // ── GET /api/v1/invitations ──────────────────────────────────────────────

    @Test
    void getAllInvitations_emptyList_returns200WithEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getAllInvitations_withRecords_returnsCorrectCount() throws Exception {
        createInvitation();
        createInvitation();

        mockMvc.perform(get("/api/v1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void getAllInvitations_registrationLinkIsNull() throws Exception {
        createInvitation();

        mockMvc.perform(get("/api/v1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].registrationLink").doesNotExist());
    }

    // ── GET /api/v1/invitations/{token} ──────────────────────────────────────

    @Test
    void getInvitationByToken_validToken_returns200() throws Exception {
        String token = createInvitation();

        mockMvc.perform(get("/api/v1/invitations/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getInvitationByToken_invalidToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
