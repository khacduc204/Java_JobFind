package com.example.JobFinder.repository;

import com.example.JobFinder.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    
    // Get application with all details
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Integer id);
    
    // Count applications by status
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);
    
    // Get all applications with filters for admin
       @Query("SELECT a FROM Application a " +
                 "LEFT JOIN FETCH a.job j " +
                 "LEFT JOIN FETCH j.employer e " +
                 "LEFT JOIN FETCH a.candidate c " +
                 "LEFT JOIN FETCH c.user u " +
                 "WHERE (:keyword IS NULL OR :keyword = '' OR " +
                 "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                 "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                 "LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                 "AND (:status IS NULL OR :status = '' OR a.status = :status) " +
                 "AND (:jobId IS NULL OR j.id = :jobId) " +
                 "AND (:employerId IS NULL OR e.id = :employerId) " +
                 "AND (:dateFrom IS NULL OR a.appliedAt >= :dateFrom) " +
                 "AND (:dateTo IS NULL OR a.appliedAt <= :dateTo) " +
                 "ORDER BY a.appliedAt DESC")
       Page<Application> findAllWithFilters(
              @Param("keyword") String keyword,
              @Param("status") String status,
              @Param("jobId") Integer jobId,
              @Param("employerId") Integer employerId,
              @Param("dateFrom") LocalDateTime dateFrom,
              @Param("dateTo") LocalDateTime dateTo,
              Pageable pageable
       );
    
    // Count applications in last N days
    @Query("SELECT COUNT(a) FROM Application a WHERE a.appliedAt >= :since")
    long countSince(@Param("since") LocalDateTime since);
    
    // Count applications by candidate
    long countByCandidateId(Integer candidateId);
    
    // Count applications by job
    long countByJobId(Integer jobId);
    
    // Get applications by candidate
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer e " +
           "WHERE a.candidate.id = :candidateId " +
           "ORDER BY a.appliedAt DESC")
    List<Application> findByCandidateId(@Param("candidateId") Integer candidateId);
    
    // Get applications by job
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE a.job.id = :jobId " +
           "ORDER BY a.appliedAt DESC")
    List<Application> findByJobId(@Param("jobId") Integer jobId);
    
    // Check if candidate already applied to job
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.candidate.id = :candidateId AND a.job.id = :jobId")
    boolean existsByCandidateIdAndJobId(@Param("candidateId") Integer candidateId, @Param("jobId") Integer jobId);
    
    // Get recent applications
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE a.appliedAt >= :since " +
           "ORDER BY a.appliedAt DESC")
    List<Application> findRecentApplications(@Param("since") LocalDateTime since, Pageable pageable);
    
    // Count applications by employer in time range
    @Query("SELECT COUNT(a) FROM Application a " +
           "WHERE a.job.employer.id = :employerId " +
           "AND a.appliedAt >= :since")
    Long countByJobEmployerIdAndAppliedAtAfter(
        @Param("employerId") Integer employerId,
        @Param("since") LocalDateTime since
    );
    
    // Get latest applications for employer with candidate info
    @Query("SELECT a.id, u.name, u.email, j.title, a.appliedAt " +
           "FROM Application a " +
           "JOIN a.job j " +
           "JOIN a.candidate c " +
           "JOIN c.user u " +
           "WHERE j.employer.id = :employerId " +
           "ORDER BY a.appliedAt DESC")
    List<Object[]> findLatestApplicationsByEmployerId(@Param("employerId") Integer employerId);
    
    // Get applications by job employer with pagination
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE j.employer.id = :employerId " +
           "ORDER BY a.appliedAt DESC")
    Page<Application> findByJobEmployerIdWithDetails(@Param("employerId") Integer employerId, Pageable pageable);
    
    // Get applications by job with pagination
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE j.id = :jobId " +
           "ORDER BY a.appliedAt DESC")
    Page<Application> findByJobIdWithDetails(@Param("jobId") Integer jobId, Pageable pageable);
    
    // Get applications by employer and status
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE j.employer.id = :employerId AND a.status = :status " +
           "ORDER BY a.appliedAt DESC")
    Page<Application> findByJobEmployerIdAndStatus(
        @Param("employerId") Integer employerId,
        @Param("status") String status,
        Pageable pageable
    );
    
    // Get application with full details for employer view
    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH j.categories " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH c.user u " +
           "WHERE a.id = :id")
    Optional<Application> findByIdWithFullDetails(@Param("id") Integer id);
    
    // Count applications by status per month
    @Query("SELECT YEAR(a.appliedAt) AS yr, MONTH(a.appliedAt) AS mon, a.status AS status, COUNT(a) AS total " +
           "FROM Application a " +
           "WHERE a.appliedAt IS NOT NULL AND a.appliedAt >= :start " +
           "GROUP BY YEAR(a.appliedAt), MONTH(a.appliedAt), a.status " +
           "ORDER BY yr, mon")
    List<Object[]> countApplicationsByStatusPerMonth(@Param("start") LocalDateTime start);
}
