package com.codehaja.auth.repository;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.entity.UserRole;
import com.codehaja.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByPasswordResetToken(String passwordResetToken);

    @Query("""
        SELECT u FROM User u
        WHERE (:searchLike IS NULL OR LOWER(u.name) LIKE :searchLike OR LOWER(u.email) LIKE :searchLike)
          AND (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
    """)
    Page<User> searchUsers(
            @Param("searchLike") String searchLike,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
