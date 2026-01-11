package com.example.JobFinder.repository;

import com.example.JobFinder.model.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    Optional<Category> findByName(String name);
    
    @Query("SELECT c FROM Category c ORDER BY c.name ASC")
    List<Category> findAllOrderByName();
    
    boolean existsByName(String name);

    @Query("""
            SELECT c.id AS id,
                   c.name AS name,
                   COUNT(DISTINCT j.id) AS jobCount
            FROM Category c
            LEFT JOIN c.jobs j WITH j.status = 'published' AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
            GROUP BY c.id, c.name
            HAVING COUNT(DISTINCT j.id) > 0
            ORDER BY COUNT(DISTINCT j.id) DESC, c.name ASC
            """)
    List<CategoryStatsProjection> findTopCategoriesWithJobCounts(Pageable pageable);

    interface CategoryStatsProjection {
        Integer getId();
        String getName();
        Long getJobCount();
    }
}
