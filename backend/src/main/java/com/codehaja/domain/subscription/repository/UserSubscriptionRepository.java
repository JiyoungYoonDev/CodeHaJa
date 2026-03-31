package com.codehaja.domain.subscription.repository;

import com.codehaja.domain.subscription.entity.SubscriptionStatus;
import com.codehaja.domain.subscription.entity.UserSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<UserSubscription> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<UserSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    Optional<UserSubscription> findTopByUserIdAndStatusOrderByExpiresAtDesc(Long userId, SubscriptionStatus status);

    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);

    @Query("SELECT COUNT(s) FROM UserSubscription s WHERE s.status = :status")
    long countByStatus(@Param("status") SubscriptionStatus status);
}
