package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ActivityResponse {
    private LocalDate date;
    private Integer count; // number of problems solved
    private Integer level; // 0-4 for heatmap intensity
}
