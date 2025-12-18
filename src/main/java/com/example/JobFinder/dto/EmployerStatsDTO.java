package com.example.JobFinder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerStatsDTO {
    private long totalEmployers;
    private long activeEmployers;
    private long inactiveEmployers;
    private long totalPublishedJobs;
}
