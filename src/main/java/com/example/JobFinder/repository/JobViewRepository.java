package com.example.JobFinder.repository;

import com.example.JobFinder.model.JobView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobViewRepository extends JpaRepository<JobView, Integer> {
    
    /**
     * Count total views for a job
     */
    @Query("SELECT COUNT(jv) FROM JobView jv WHERE jv.job.id = :jobId")
    long countByJobId(@Param("jobId") Integer jobId);

    @Query("SELECT COUNT(jv) FROM JobView jv WHERE jv.job.employer.id = :employerId")
    long countByEmployerId(@Param("employerId") Integer employerId);
    
    /**
     * Check if IP already viewed a job (to prevent duplicate counting in same session)
     */
    @Query("SELECT COUNT(jv) FROM JobView jv WHERE jv.job.id = :jobId AND jv.viewerIp = :ip AND jv.viewedAt > :since")
    long countRecentViewByJobIdAndIp(@Param("jobId") Integer jobId, @Param("ip") String ip, @Param("since") java.time.LocalDateTime since);
}
