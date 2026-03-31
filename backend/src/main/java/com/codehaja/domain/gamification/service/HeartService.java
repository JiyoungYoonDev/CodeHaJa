package com.codehaja.domain.gamification.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.subscription.entity.SubscriptionStatus;
import com.codehaja.domain.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HeartService {

    public static final int MAX_HEARTS = 5;
    public static final int REFILL_HOURS = 4;

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    /**
     * Throws HEART_EMPTY if user has no hearts (and is not Pro).
     * Call this BEFORE a submission attempt.
     */
    @Transactional
    public void requireHeart(User user) {
        if (isPro(user)) return;
        tryAutoRefill(user);
        if (user.getHearts() <= 0) {
            throw new BusinessException(ErrorCode.HEART_EMPTY);
        }
    }

    /**
     * Deducts 1 heart on wrong answer. No-op for Pro users.
     * Sets heartsRefillAt when hearts reach 0.
     */
    @Transactional
    public void deductHeart(User user) {
        if (isPro(user)) return;
        tryAutoRefill(user);
        if (user.getHearts() > 0) {
            user.setHearts(user.getHearts() - 1);
        }
        if (user.getHearts() == 0 && user.getHeartsRefillAt() == null) {
            user.setHeartsRefillAt(LocalDateTime.now().plusHours(REFILL_HOURS));
        }
        userRepository.save(user);
    }

    private void tryAutoRefill(User user) {
        if (user.getHearts() < MAX_HEARTS
                && user.getHeartsRefillAt() != null
                && LocalDateTime.now().isAfter(user.getHeartsRefillAt())) {
            user.setHearts(MAX_HEARTS);
            user.setHeartsRefillAt(null);
        }
    }

    private boolean isPro(User user) {
        return subscriptionRepository.existsByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);
    }
}
