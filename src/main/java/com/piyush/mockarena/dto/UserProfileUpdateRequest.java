// UserProfileUpdateRequest.java
package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String fullName;
    private String bio;
    private String githubUsername;
    private String linkedinUrl;
    private String currentCompany;
    private String currentPosition;
    private String location;
    private String websiteUrl;
    private String preferredLanguage;
    private Boolean isPublicProfile;
}
