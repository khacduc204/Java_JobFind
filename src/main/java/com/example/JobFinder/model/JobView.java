package com.example.JobFinder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(name = "viewer_ip", length = 45)
    private String viewerIp;
    
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
    
    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}
