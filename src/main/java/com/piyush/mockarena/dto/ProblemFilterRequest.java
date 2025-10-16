// ProblemFilterRequest.java
package com.piyush.mockarena.dto;

import com.piyush.mockarena.entity.Problem;
import lombok.Data;
import java.util.List;

@Data
public class ProblemFilterRequest {
    private String search;
    private List<Problem.Difficulty> difficulties;
    private List<Long> tagIds;
    private String company;
    private Boolean premium;
}
