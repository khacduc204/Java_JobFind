package com.example.JobFinder.repository;

import com.example.JobFinder.model.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Integer> {
    
    // Find saved job by candidate and job
    Optional<SavedJob> findByCandidateIdAndJobId(Integer candidateId, Integer jobId);
    
    // Check if job is saved by candidate
    boolean existsByCandidateIdAndJobId(Integer candidateId, Integer jobId);
    
    // Get all saved job IDs for candidate
    @Query("SELECT sj.job.id FROM SavedJob sj WHERE sj.candidate.id = :candidateId")
    List<Integer> findJobIdsByCandidateId(@Param("candidateId") Integer candidateId);
    
    // Count saved jobs by candidate
    long countByCandidateId(Integer candidateId);
    
    // Delete saved job by candidate and job
    void deleteByCandidateIdAndJobId(Integer candidateId, Integer jobId);
}
