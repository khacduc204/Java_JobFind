package com.example.JobFinder.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApplicationDetailView {
    Integer applicationId;
    String status;
    String appliedAtFormatted;

    String candidateName;
    String candidateEmail;
    String candidatePhone;
    String candidateHeadline;
    String candidateSummary;
    String candidateExperience;
    String candidateLocation;
    String candidateSkills;

    String coverLetter;
    String resumeSnapshot;

    String jobTitle;
    String jobLocation;
    String jobEmploymentType;
    String jobSalary;
    String jobDescription;
    String jobRequirements;
}
