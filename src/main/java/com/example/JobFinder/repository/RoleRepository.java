package com.example.JobFinder.repository;

import com.example.JobFinder.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByNameIgnoreCase(String name);
}
