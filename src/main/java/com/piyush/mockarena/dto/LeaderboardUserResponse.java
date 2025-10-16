package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class LeaderboardUserResponse {
    private Integer rank;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private Integer contestRating;
    private Integer totalProblemsSolved;
    private Double acceptanceRate;
    private String currentCompany;
    private String location;
}
