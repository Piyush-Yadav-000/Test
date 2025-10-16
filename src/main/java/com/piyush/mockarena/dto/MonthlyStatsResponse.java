package com.piyush.mockarena.dto;

import lombok.Data;
import java.time.YearMonth;

@Data
public class MonthlyStatsResponse {
    private YearMonth month;
    private Integer problemsSolved;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;
}
