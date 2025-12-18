package com.example.JobFinder.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 255)
    private String headline;

    @Lob
    private String summary;

    @Column(length = 255)
    private String location;

    @Lob
    private String skills;

    @Lob
    private String experience;

    @Column(name = "profile_picture", length = 255)
    private String profilePicture;

    @Column(name = "cv_path", length = 255)
    private String cvPath;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
