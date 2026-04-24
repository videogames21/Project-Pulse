package edu.tcu.cs.projectpulse.invitation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.tcu.cs.projectpulse.TestJwtHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class InvitationControllerIntegrationTest {

    @Autowired WebApplicationContext wac;
    @Autowired InvitationRepository invitationRepository;
    @Autowired TestJwtHelper jwtHelper;

    ObjectMapper objectMapper = new ObjectMapper();
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        invitationRepository.deleteAll();
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private String createInvitation() throws Exception {
        String response = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andReturn().getResponse().getContentAsString();
        String link = objectMapper.readTree(response).path("data").path("registrationLink").asText();
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private void disableInvitation(String token) throws Exception {
        mockMvc.perform(patch("/api/v1/invitations/" + token + "/disable")
                .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk());
    }

    // ── POST /api/v1/invitations ─────────────────────────────────────────────

    @Test
    void generateInvitation_success_returns200WithLink() throws Exception {
        mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationLink", startsWith("http://localhost:3000/register/")))
                .andExpect(jsonPath("$.data.tokenShort", hasLength(8)))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.invitedBy").value("admin@tcu.edu"))
                .andExpect(jsonPath("$.data.usageCount").value(0))
                .andExpect(jsonPath("$.data.acceptedUsers", hasSize(0)));
    }

    @Test
    void generateInvitation_withoutJwt_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/invitations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void generateInvitation_twoCalls_produceDifferentTokens() throws Exception {
        String token1 = createInvitation();
        String token2 = createInvitation();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateInvitation_persistsToDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk());

        assertThat(invitationRepository.count()).isEqualTo(1);
    }

    // ── GET /api/v1/invitations ──────────────────────────────────────────────

    @Test
    void getAllInvitations_emptyList_returns200WithEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getAllInvitations_withoutJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/invitations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllInvitations_withRecords_returnsCorrectCount() throws Exception {
        createInvitation();
        createInvitation();

        mockMvc.perform(get("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void getAllInvitations_includesRegistrationLink() throws Exception {
        createInvitation();

        mockMvc.perform(get("/api/v1/invitations")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].registrationLink", startsWith("http://localhost:3000/register/")));
    }

    // ── GET /api/v1/invitations/{token} ──────────────────────────────────────

    @Test
    void getInvitationByToken_validToken_returns200() throws Exception {
        String token = createInvitation();

        mockMvc.perform(get("/api/v1/invitations/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void getInvitationByToken_invalidToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PATCH /api/v1/invitations/{token}/disable ────────────────────────────

    @Test
    void disableInvitation_success_returns200WithDisabledStatus() throws Exception {
        String token = createInvitation();

        mockMvc.perform(patch("/api/v1/invitations/" + token + "/disable")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void disableInvitation_alreadyDisabled_returns409() throws Exception {
        String token = createInvitation();
        disableInvitation(token);

        mockMvc.perform(patch("/api/v1/invitations/" + token + "/disable")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void disableInvitation_withoutJwt_returns403() throws Exception {
        String token = createInvitation();

        mockMvc.perform(patch("/api/v1/invitations/" + token + "/disable"))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/v1/invitations/{token}/enable ─────────────────────────────

    @Test
    void enableInvitation_success_returns200WithActiveStatus() throws Exception {
        String token = createInvitation();
        disableInvitation(token);

        mockMvc.perform(patch("/api/v1/invitations/" + token + "/enable")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void enableInvitation_alreadyActive_returns409() throws Exception {
        String token = createInvitation();

        mockMvc.perform(patch("/api/v1/invitations/" + token + "/enable")
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isConflict());
    }

    // ── DELETE /api/v1/invitations/{token} ───────────────────────────────────

    @Test
    void deleteInvitation_whenDisabled_returns200AndRemovedFromDb() throws Exception {
        String token = createInvitation();
        disableInvitation(token);

        mockMvc.perform(delete("/api/v1/invitations/" + token)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(invitationRepository.count()).isEqualTo(0);
    }

    @Test
    void deleteInvitation_whenActive_returns409() throws Exception {
        String token = createInvitation();

        mockMvc.perform(delete("/api/v1/invitations/" + token)
                        .header("Authorization", jwtHelper.adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteInvitation_withoutJwt_returns403() throws Exception {
        String token = createInvitation();
        disableInvitation(token);

        mockMvc.perform(delete("/api/v1/invitations/" + token))
                .andExpect(status().isForbidden());
    }
}
