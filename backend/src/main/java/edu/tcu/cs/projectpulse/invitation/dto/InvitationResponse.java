package edu.tcu.cs.projectpulse.invitation.dto;

public class InvitationResponse {

    private Long id;
    private String registrationLink;  // populated on POST only; null in list view
    private String tokenShort;        // first 8 chars of token for table display
    private String status;
    private String createdAt;
    private String invitedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRegistrationLink() { return registrationLink; }
    public void setRegistrationLink(String registrationLink) { this.registrationLink = registrationLink; }

    public String getTokenShort() { return tokenShort; }
    public void setTokenShort(String tokenShort) { this.tokenShort = tokenShort; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }
}
