package com.codehaja.domain.anonymous.repository;

import com.codehaja.domain.anonymous.entity.AnonymousUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnonymousUserRepository extends JpaRepository<AnonymousUser, Long> {
    Optional<AnonymousUser> findByAnonymousUserKey(String anonymousUserKey);
}