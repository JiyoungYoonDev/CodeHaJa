package com.codehaja.domain.anonymous.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.anonymous.dto.AnonymousUserDto;
import com.codehaja.domain.anonymous.entity.AnonymousUser;
import com.codehaja.domain.anonymous.repository.AnonymousUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnonymousUserService {

    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public AnonymousUserDto.Response initAnonymousUser(AnonymousUserDto.InitRequest request) {
        String key = request == null ? null : request.getAnonymousUserKey();

        if (key == null || key.isBlank()) {
            key = UUID.randomUUID().toString();
        }

        final String finalKey = key;
        AnonymousUser anonymousUser = anonymousUserRepository.findByAnonymousUserKey(finalKey)
                .orElseGet(() -> {
                    AnonymousUser newUser = new AnonymousUser();
                    newUser.setAnonymousUserKey(finalKey);
                    return anonymousUserRepository.save(newUser);
                });

        anonymousUser.setLastSeenAt(LocalDateTime.now());

        AnonymousUserDto.Response response = new AnonymousUserDto.Response();
        response.setId(anonymousUser.getId());
        response.setAnonymousUserKey(anonymousUser.getAnonymousUserKey());
        return response;
    }

    @Transactional
    public AnonymousUser getOrCreateAnonymousUser(String anonymousUserKey) {
        if (anonymousUserKey == null || anonymousUserKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "anonymousUserKey is required.");
        }
        return anonymousUserRepository.findByAnonymousUserKey(anonymousUserKey)
                .orElseGet(() -> {
                    AnonymousUser newUser = new AnonymousUser();
                    newUser.setAnonymousUserKey(anonymousUserKey);
                    return anonymousUserRepository.save(newUser);
                });
    }

    public AnonymousUser getAnonymousUserOrThrow(String anonymousUserKey) {
        if (anonymousUserKey == null || anonymousUserKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "anonymousUserKey is required.");
        }

        return anonymousUserRepository.findByAnonymousUserKey(anonymousUserKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANONYMOUS_USER_NOT_FOUND));
    }
}