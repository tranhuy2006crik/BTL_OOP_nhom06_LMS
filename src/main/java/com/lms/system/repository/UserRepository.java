package com.lms.system.repository;

import com.lms.system.model.User;
import com.lms.system.model.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByStatusOrderByFullNameAsc(UserStatus status);

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
            WHERE (:query = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:status IS NULL OR u.status = :status)
            ORDER BY LOWER(u.fullName), u.id
            """)
    Page<User> searchAdminUsers(
            @Param("query") String query,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    long countByStatus(UserStatus status);

    @Query("""
            SELECT COUNT(DISTINCT u) FROM User u
            JOIN u.systemRoleAssignments a
            JOIN a.role r
            WHERE u.status = com.lms.system.model.UserStatus.ACTIVE
              AND r.code = :roleCode
            """)
    long countActiveUsersWithSystemRole(@Param("roleCode") String roleCode);

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
