package com.lms.system.repository;

import com.lms.system.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"systemRoleAssignments.role", "courseMemberships.role"})
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findWithAccessByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"systemRoleAssignments.role", "courseMemberships.role"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findWithAccessById(@Param("id") Long id);
}
