package edu.tcu.cs.projectpulse.invitation.dto;

import java.util.List;

public class InvitationResponse {

    private Long id;
    private String token;
    private String registrationLink;
    private String tokenShort;
    private String status;
    private String createdAt;
    private String invitedBy;
    private String email;
    private int usageCount;
    private List<AcceptedUserInfo> acceptedUsers;
    private String role;
    private String accessCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public List<AcceptedUserInfo> getAcceptedUsers() { return acceptedUsers; }
    public void setAcceptedUsers(List<AcceptedUserInfo> acceptedUsers) { this.acceptedUsers = acceptedUsers; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
}
