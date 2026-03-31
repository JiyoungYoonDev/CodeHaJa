package com.codehaja.auth.cms.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.subscription.dto.SubscriptionDto;
import com.codehaja.domain.subscription.entity.SubscriptionStatus;
import com.codehaja.domain.subscription.entity.UserSubscription;
import com.codehaja.domain.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsSubscriptionService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public Page<SubscriptionDto.SubscriptionListItem> getSubscriptions(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserSubscription> subs = (status != null && !status.isBlank())
                ? subscriptionRepository.findByStatus(SubscriptionStatus.valueOf(status), pageable)
                : subscriptionRepository.findAll(pageable);

        return subs.map(this::toListItem);
    }

    @Transactional
    public SubscriptionDto.SubscriptionListItem grantSubscription(SubscriptionDto.GrantRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(user.getId(), SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                });

        UserSubscription sub = new UserSubscription();
        sub.setUser(user);
        sub.setPlan(request.getPlan());
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartedAt(LocalDateTime.now());
        sub.setExpiresAt(request.getExpiresAt());

        return toListItem(subscriptionRepository.save(sub));
    }

    @Transactional
    public SubscriptionDto.SubscriptionListItem cancelSubscription(Long subscriptionId) {
        UserSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        sub.setStatus(SubscriptionStatus.CANCELLED);
        return toListItem(subscriptionRepository.save(sub));
    }

    public SubscriptionDto.SubscriptionInfo getActiveSubscription(Long userId) {
        return subscriptionRepository
                .findTopByUserIdAndStatusOrderByExpiresAtDesc(userId, SubscriptionStatus.ACTIVE)
                .map(s -> SubscriptionDto.SubscriptionInfo.builder()
                        .id(s.getId())
                        .plan(s.getPlan().name())
                        .status(s.getStatus().name())
                        .startedAt(s.getStartedAt())
                        .expiresAt(s.getExpiresAt())
                        .build())
                .orElse(null);
    }

    public long countActiveSubscriptions() {
        return subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
    }

    private SubscriptionDto.SubscriptionListItem toListItem(UserSubscription s) {
        return SubscriptionDto.SubscriptionListItem.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .userName(s.getUser().getName())
                .userEmail(s.getUser().getEmail())
                .plan(s.getPlan().name())
                .status(s.getStatus().name())
                .startedAt(s.getStartedAt())
                .expiresAt(s.getExpiresAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
