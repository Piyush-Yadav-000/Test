// ProblemTagResponse.java
package com.piyush.mockarena.dto;

import lombok.Data;

@Data
public class ProblemTagResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String category;
    private String colorCode;
    private Integer sortOrder;
    private boolean active;
}
