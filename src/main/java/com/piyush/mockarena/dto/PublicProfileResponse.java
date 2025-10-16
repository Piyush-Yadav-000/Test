package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PublicProfileResponse {
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private String bio;
    private String currentCompany;
    private String currentPosition;
    private String location;
    private Integer totalProblemsSolved;
    private Integer contestRating;
    private Integer globalRank;
    private LocalDateTime joinDate;
}
