package com.example.JobFinder.repository;

import com.example.JobFinder.model.Candidate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidateRepository extends JpaRepository<Candidate, Integer> {

    @Query("SELECT c FROM Candidate c LEFT JOIN FETCH c.user WHERE c.user.id = :userId")
    Optional<Candidate> findByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT c FROM Candidate c LEFT JOIN FETCH c.user WHERE c.id = :id")
    @Override
    Optional<Candidate> findById(@Param("id") Integer id);
}
