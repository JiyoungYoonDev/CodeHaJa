package com.codehaja.domain.gamification.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class XpService {

    public static final int XP_LECTURE_ITEM_COMPLETE = 20;
    public static final int XP_CODING_PASS = 50;

    private final UserRepository userRepository;

    /**
     * Awards XP to a user, updates streak, and persists.
     * Returns the amount awarded.
     */
    @Transactional
    public int award(User user, int amount) {
        user.setTotalXp(user.getTotalXp() + amount);
        updateStreak(user);
        userRepository.save(user);
        return amount;
    }

    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastActivityDate();

        if (last == null) {
            user.setStreakDays(1);
        } else if (last.equals(today)) {
            // already active today — no change
        } else if (last.equals(today.minusDays(1))) {
            user.setStreakDays(user.getStreakDays() + 1);
        } else {
            user.setStreakDays(1);
        }
        user.setLastActivityDate(today);
    }
}
