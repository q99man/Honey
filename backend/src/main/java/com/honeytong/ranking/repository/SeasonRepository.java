package com.honeytong.ranking.repository;

import com.honeytong.ranking.entity.Season;
import com.honeytong.ranking.entity.SeasonStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    boolean existsBySeasonCode(String seasonCode);

    List<Season> findAllByOrderByStartAtDesc();

    Optional<Season> findBySeasonCode(String seasonCode);

    Optional<Season> findFirstByStatusOrderByStartAtDesc(SeasonStatus status);
}
