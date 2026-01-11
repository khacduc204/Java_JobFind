package com.example.JobFinder.repository;

import com.example.JobFinder.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    
    // Find job with employer info
    @Query("SELECT DISTINCT j FROM Job j LEFT JOIN FETCH j.employer e LEFT JOIN FETCH e.user LEFT JOIN FETCH j.categories WHERE j.id = :id")
    Optional<Job> findByIdWithDetails(@Param("id") Integer id);
    
    // Count jobs by status
    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = :status")
    long countByStatus(@Param("status") String status);
    
    // Get published jobs with pagination and filters (without FETCH for pagination)
    @Query("SELECT DISTINCT j FROM Job j " +
           "LEFT JOIN j.employer e " +
           "LEFT JOIN j.categories c " +
           "WHERE j.status = 'published' " +
           "AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')) OR LOWER(e.address) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:employmentType IS NULL OR :employmentType = '' OR j.employmentType = :employmentType) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId)")
    Page<Job> findPublishedJobsWithFilters(
        @Param("keyword") String keyword,
        @Param("location") String location,
        @Param("employmentType") String employmentType,
        @Param("categoryId") Integer categoryId,
        Pageable pageable
    );
    
    // Get featured jobs (recently posted)
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.employer e LEFT JOIN FETCH j.categories " +
           "WHERE j.status = 'published' " +
           "AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE) " +
           "ORDER BY j.createdAt DESC")
    List<Job> findFeaturedJobs(Pageable pageable);
    
       // Get jobs by employer
       @Query("SELECT j FROM Job j LEFT JOIN FETCH j.categories WHERE j.employer.id = :employerId ORDER BY j.createdAt DESC")
       List<Job> findByEmployerId(@Param("employerId") Integer employerId);

       @Query("SELECT j FROM Job j LEFT JOIN FETCH j.categories WHERE j.employer.id = :employerId AND j.status = 'published' ORDER BY COALESCE(j.updatedAt, j.createdAt) DESC")
       List<Job> findPublishedByEmployerId(@Param("employerId") Integer employerId);
    
    // Get jobs by employer with pagination
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.categories WHERE j.employer.id = :employerId")
    Page<Job> findByEmployerIdWithPage(@Param("employerId") Integer employerId, Pageable pageable);
    
    // Get jobs by employer with pagination and sorting (for job management)
    @Query(value = "SELECT j FROM Job j LEFT JOIN FETCH j.employer WHERE j.employer.id = :employerId ORDER BY j.createdAt DESC",
           countQuery = "SELECT COUNT(j) FROM Job j WHERE j.employer.id = :employerId")
    Page<Job> findByEmployerId(@Param("employerId") Integer employerId, Pageable pageable);
    
    // Count jobs by employer
    long countByEmployerId(Integer employerId);
    
    // Count jobs by employer and status
    @Query("SELECT COUNT(j) FROM Job j WHERE j.employer.id = :employerId AND j.status = :status")
    Long countByEmployerIdAndStatus(@Param("employerId") Integer employerId, @Param("status") String status);
    
    // Count published jobs by employer
    @Query("SELECT COUNT(j) FROM Job j WHERE j.employer.id = :employerId AND j.status = 'published'")
    long countPublishedByEmployerId(@Param("employerId") Integer employerId);
    
    // Get related jobs (same categories, different employer)
    @Query("SELECT DISTINCT j FROM Job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH j.categories c " +
           "WHERE j.status = 'published' " +
           "AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE) " +
           "AND j.employer.id != :employerId " +
           "AND c.id IN :categoryIds " +
           "ORDER BY j.createdAt DESC")
    List<Job> findRelatedJobs(@Param("employerId") Integer employerId, @Param("categoryIds") List<Integer> categoryIds, Pageable pageable);
    
    // Get jobs for saved jobs by candidate
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.employer e LEFT JOIN FETCH j.categories " +
           "WHERE j.id IN (SELECT sj.job.id FROM SavedJob sj WHERE sj.candidate.id = :candidateId) " +
           "ORDER BY j.createdAt DESC")
    Page<Job> findSavedJobsByCandidateId(@Param("candidateId") Integer candidateId, Pageable pageable);
    
    // Get hot jobs sorted by view count from job_views table
    @Query("SELECT j FROM Job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH j.categories " +
           "WHERE j.status = 'published' " +
           "AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE) " +
           "ORDER BY (SELECT COUNT(jv) FROM JobView jv WHERE jv.job.id = j.id) DESC, j.createdAt DESC")
    Page<Job> findHotJobsByViewCount(Pageable pageable);
    
    // Get top N jobs by view count for homepage
    @Query("SELECT j FROM Job j " +
           "LEFT JOIN FETCH j.employer e " +
           "LEFT JOIN FETCH j.categories " +
           "WHERE j.status = 'published' " +
           "AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE) " +
           "ORDER BY (SELECT COUNT(jv) FROM JobView jv WHERE jv.job.id = j.id) DESC, j.createdAt DESC")
    List<Job> findTopJobsByViewCount(Pageable pageable);

    @Query("SELECT YEAR(j.createdAt) AS yr, MONTH(j.createdAt) AS mon, COUNT(j) AS total " +
           "FROM Job j " +
           "WHERE j.createdAt IS NOT NULL AND j.createdAt >= :start " +
           "GROUP BY YEAR(j.createdAt), MONTH(j.createdAt) " +
           "ORDER BY yr, mon")
    List<Object[]> countJobsGroupedByMonth(@Param("start") LocalDateTime start);

    @Query("""
            SELECT DISTINCT j.title
            FROM Job j
            WHERE j.status = 'published'
              AND j.title IS NOT NULL
              AND j.title <> ''
              AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
            ORDER BY j.createdAt DESC
            """)
    List<String> findRecentJobTitles(Pageable pageable);

       // Recommendation fallback for MariaDB: prioritize location match, then recency
       @Query(value = """
                     SELECT j.id,
                               CASE
                                      WHEN :location IS NOT NULL AND j.location IS NOT NULL AND LOWER(j.location) LIKE CONCAT('%', LOWER(:location), '%') THEN 5
                                      ELSE 0
                               END AS match_score
                     FROM jobs j
                     WHERE j.status = 'published'
                       AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
                     ORDER BY match_score DESC, j.created_at DESC
                     """,
                     countQuery = """
                            SELECT COUNT(*) FROM jobs j
                            WHERE j.status = 'published'
                              AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
                     """,
                     nativeQuery = true)
       List<Object[]> findRecommendedJobIds(@Param("candidateId") Integer candidateId, @Param("location") String location, Pageable pageable);

       // Fetch jobs with employer, user, categories by IDs
       @Query("SELECT DISTINCT j FROM Job j " +
                 "LEFT JOIN FETCH j.employer e " +
                 "LEFT JOIN FETCH e.user " +
                 "LEFT JOIN FETCH j.categories " +
                 "WHERE j.id IN :ids")
       List<Job> findByIdInWithDetails(@Param("ids") List<Integer> ids);
}
