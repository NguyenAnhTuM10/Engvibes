package com.englishapp.shadow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserPhonemeStatsRepository extends JpaRepository<UserPhonemeStats, UserPhonemeStatsId> {

    List<UserPhonemeStats> findByIdUserId(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_phoneme_stats (user_id, phoneme, total_attempts, errors, last_updated)
            VALUES (:userId, :phoneme, :totalDelta, :errorDelta, NOW())
            ON CONFLICT (user_id, phoneme) DO UPDATE SET
                total_attempts = user_phoneme_stats.total_attempts + :totalDelta,
                errors = user_phoneme_stats.errors + :errorDelta,
                last_updated = NOW()
            """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId,
                @Param("phoneme") String phoneme,
                @Param("totalDelta") int totalDelta,
                @Param("errorDelta") int errorDelta);
}
