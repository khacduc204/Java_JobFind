package com.example.JobFinder.repository;

import com.example.JobFinder.model.Employer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Integer> {

    @Query("SELECT DISTINCT e FROM Employer e LEFT JOIN FETCH e.user ORDER BY e.id DESC")
    List<Employer> findAllWithUser();

    @Query("SELECT e FROM Employer e LEFT JOIN FETCH e.user WHERE e.id = :id")
    Optional<Employer> findByIdWithUser(@Param("id") Integer id);

    Optional<Employer> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);

    @Query("SELECT e FROM Employer e LEFT JOIN FETCH e.user u " +
           "WHERE (:keyword = '' OR e.companyName LIKE %:keyword% OR e.address LIKE %:keyword% OR u.email LIKE %:keyword% OR u.name LIKE %:keyword%) " +
           "AND (:location = '' OR e.address LIKE %:location%) " +
           "ORDER BY e.id DESC")
    List<Employer> findByFilters(@Param("keyword") String keyword, @Param("location") String location);

    // For public employer directory with pagination
    @Query("SELECT e FROM Employer e " +
           "WHERE (:searchTerm IS NULL OR :searchTerm = '' OR e.companyName LIKE %:searchTerm% OR e.address LIKE %:searchTerm%) " +
           "AND (:location IS NULL OR :location = '' OR e.address LIKE %:location%)")
    Page<Employer> findByFilters(@Param("searchTerm") String searchTerm, @Param("location") String location, Pageable pageable);
    
    Optional<Employer> findById(Long id);
}
