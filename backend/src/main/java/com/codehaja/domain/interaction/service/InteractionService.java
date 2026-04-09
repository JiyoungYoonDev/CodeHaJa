package com.codehaja.domain.interaction.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.domain.interaction.dto.InteractionDto;
import com.codehaja.domain.interaction.entity.HintUsage;
import com.codehaja.domain.interaction.entity.LectureItemReaction;
import com.codehaja.domain.interaction.entity.ReactionType;
import com.codehaja.domain.interaction.repository.HintUsageRepository;
import com.codehaja.domain.interaction.repository.LectureItemReactionRepository;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionService {

    public static final int XP_PENALTY_PER_HINT = 5;

    private final UserRepository userRepository;
    private final LectureItemRepository lectureItemRepository;
    private final HintUsageRepository hintUsageRepository;
    private final LectureItemReactionRepository reactionRepository;

    // ── Hints ──

    @Transactional
    public InteractionDto.HintStatusResponse revealHint(Long lectureItemId, int hintIndex, String email) {
        User user = findUser(email);
        LectureItem item = findItem(lectureItemId);

        if (!hintUsageRepository.existsByUserIdAndLectureItemIdAndHintIndex(user.getId(), lectureItemId, hintIndex)) {
            HintUsage usage = new HintUsage();
            usage.setUser(user);
            usage.setLectureItem(item);
            usage.setHintIndex(hintIndex);
            hintUsageRepository.save(usage);
        }

        return buildHintStatus(user.getId(), lectureItemId);
    }

    public InteractionDto.HintStatusResponse getHintStatus(Long lectureItemId, String email) {
        User user = findUser(email);
        return buildHintStatus(user.getId(), lectureItemId);
    }

    public int getHintXpPenalty(Long userId, Long lectureItemId) {
        int count = hintUsageRepository.countByUserIdAndLectureItemId(userId, lectureItemId);
        return count * XP_PENALTY_PER_HINT;
    }

    private InteractionDto.HintStatusResponse buildHintStatus(Long userId, Long lectureItemId) {
        List<HintUsage> usages = hintUsageRepository.findByUserIdAndLectureItemId(userId, lectureItemId);
        List<Integer> indexes = usages.stream().map(HintUsage::getHintIndex).sorted().toList();
        return new InteractionDto.HintStatusResponse(
                lectureItemId,
                indexes,
                indexes.size(),
                indexes.size() * XP_PENALTY_PER_HINT
        );
    }

    // ── Reactions (like/dislike) ──

    @Transactional
    public InteractionDto.ReactionStatusResponse react(Long lectureItemId, String reactionTypeStr, String email) {
        User user = findUser(email);
        LectureItem item = findItem(lectureItemId);
        ReactionType reactionType = ReactionType.valueOf(reactionTypeStr.toUpperCase());

        LectureItemReaction reaction = reactionRepository
                .findByUserIdAndLectureItemId(user.getId(), lectureItemId)
                .orElse(null);

        if (reaction != null) {
            if (reaction.getReactionType() == reactionType) {
                // Toggle off — remove reaction
                reactionRepository.delete(reaction);
            } else {
                // Switch reaction
                reaction.setReactionType(reactionType);
                reactionRepository.save(reaction);
            }
        } else {
            reaction = new LectureItemReaction();
            reaction.setUser(user);
            reaction.setLectureItem(item);
            reaction.setReactionType(reactionType);
            reactionRepository.save(reaction);
        }

        return buildReactionStatus(user.getId(), lectureItemId);
    }

    public InteractionDto.ReactionStatusResponse getReactionStatus(Long lectureItemId, String email) {
        User user = findUser(email);
        return buildReactionStatus(user.getId(), lectureItemId);
    }

    private InteractionDto.ReactionStatusResponse buildReactionStatus(Long userId, Long lectureItemId) {
        LectureItemReaction userReaction = reactionRepository
                .findByUserIdAndLectureItemId(userId, lectureItemId)
                .orElse(null);

        long likes = reactionRepository.countByLectureItemIdAndReactionType(lectureItemId, ReactionType.LIKE);
        long dislikes = reactionRepository.countByLectureItemIdAndReactionType(lectureItemId, ReactionType.DISLIKE);

        return new InteractionDto.ReactionStatusResponse(
                lectureItemId,
                userReaction != null ? userReaction.getReactionType().name() : null,
                likes,
                dislikes
        );
    }

    // ── Helpers ──

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private LectureItem findItem(Long id) {
        return lectureItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LectureItem not found: " + id));
    }
}
