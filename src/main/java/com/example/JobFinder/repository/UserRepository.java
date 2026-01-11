package com.example.JobFinder.repository;

import com.example.JobFinder.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role r LEFT JOIN FETCH r.permissions WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.role ORDER BY u.id")
    @Override
    @NonNull
    List<User> findAll();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.id = :roleId")
    long countByRoleId(@Param("roleId") Integer roleId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.role.id = :roleId ORDER BY u.id")
    List<User> findByRoleId(@Param("roleId") Integer roleId);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u.role.id AS roleId, COUNT(u.id) AS userCount FROM User u WHERE u.role.id IN :roleIds GROUP BY u.role.id")
    List<RoleUserCount> countUsersByRoleIds(@Param("roleIds") List<Integer> roleIds);

    interface RoleUserCount {

        Integer getRoleId();

        long getUserCount();
    }
}
