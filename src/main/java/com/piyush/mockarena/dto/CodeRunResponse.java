package com.piyush.mockarena.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunResponse {
    private String status;
    private String stdout;
    private String stderr;
    private String compileOutput;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String message;
    private String language;
    private String executionTime;
}
