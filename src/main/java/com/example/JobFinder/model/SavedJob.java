package com.example.JobFinder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_jobs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"candidate_id", "job_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(name = "saved_at")
    private LocalDateTime savedAt;
    
    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}
