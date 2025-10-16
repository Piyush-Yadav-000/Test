// ProblemTagRequest.java
package com.piyush.mockarena.dto;

import com.piyush.mockarena.entity.ProblemTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProblemTagRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String description;

    @NotNull(message = "Category is required")
    private ProblemTag.TagCategory category;

    private String colorCode;
    private Integer sortOrder = 0;
}
