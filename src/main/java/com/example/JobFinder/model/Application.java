package com.example.JobFinder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;
    
    @Column(name = "resume_snapshot", columnDefinition = "TEXT")
    private String resumeSnapshot;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status = "applied"; // applied, viewed, shortlisted, rejected, hired
    
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    
    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
    }
}
