package com.thesamet.gerrit.plugins.saml;

public class AuthenticatedUser implements java.io.Serializable {
    private String username;
    private String displayName;
    private String email;
    private String externalId;

    public AuthenticatedUser(String username, String displayName, String email, String extenalId) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.externalId = extenalId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", externalId='" + externalId + '\'' +
                '}';
    }
}
