package com.codehaja.auth.cms.service;

import com.codehaja.auth.cms.dto.CmsUserDto;
import com.codehaja.auth.entity.User;
import com.codehaja.auth.entity.UserRole;
import com.codehaja.auth.entity.UserStatus;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.enrollment.repository.CourseEnrollmentRepository;
import com.codehaja.domain.subscription.entity.SubscriptionStatus;
import com.codehaja.domain.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsUserService {

    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    public Page<CmsUserDto.UserListItem> getUsers(
            String search, String role, String status, int page, int size) {

        UserRole roleEnum = (role != null && !role.isBlank()) ? UserRole.valueOf(role) : null;
        UserStatus statusEnum = (status != null && !status.isBlank()) ? UserStatus.valueOf(status) : null;
        String searchLike = (search != null && !search.isBlank()) ? "%" + search.toLowerCase() + "%" : null;

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return userRepository.searchUsers(searchLike, roleEnum, statusEnum, pageable)
                .map(this::toListItem);
    }

    @Transactional
    public CmsUserDto.UserListItem updateRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        user.setRole(role);
        return toListItem(userRepository.save(user));
    }

    @Transactional
    public CmsUserDto.UserListItem updateStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        user.setStatus(status);
        return toListItem(userRepository.save(user));
    }

    private CmsUserDto.UserListItem toListItem(User u) {
        var sub = subscriptionRepository
                .findTopByUserIdAndStatusOrderByExpiresAtDesc(u.getId(), SubscriptionStatus.ACTIVE)
                .orElse(null);
        return CmsUserDto.UserListItem.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .status(u.getStatus().name())
                .provider(u.getProvider().name())
                .createdAt(u.getCreatedAt())
                .enrollmentCount(enrollmentRepository.countByUserId(u.getId()))
                .subscribed(sub != null)
                .subscriptionPlan(sub != null ? sub.getPlan().name() : null)
                .subscriptionStatus(sub != null ? sub.getStatus().name() : null)
                .subscriptionExpiresAt(sub != null ? sub.getExpiresAt() : null)
                .build();
    }
}
