package com.stats.repository;

import com.stats.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    Optional<UserStats> findByUserId(Long userId);
    List<UserStats> findTop10ByOrderByWinsDesc();
}