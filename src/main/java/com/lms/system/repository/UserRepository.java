package com.lms.system.repository;

import com.lms.system.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.lms.system.model.UserStatus;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"systemRoleAssignments.role", "courseMemberships.role", "profile"})
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findWithAccessByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"systemRoleAssignments.role", "courseMemberships.role", "profile"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findWithAccessById(@Param("id") Long id);

    @Query("""
            SELECT u FROM User u
            WHERE u.status = :status
              AND u.id <> :currentUserId
              AND (:query = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY LOWER(u.fullName), u.id
            """)
    Page<User> searchMessageContacts(
            @Param("currentUserId") Long currentUserId,
            @Param("status") UserStatus status,
            @Param("query") String query,
            Pageable pageable
    );
}
