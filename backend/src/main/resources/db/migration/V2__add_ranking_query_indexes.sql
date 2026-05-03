ALTER TABLE place_season_scores
  ADD INDEX idx_place_season_scores_region_rank (
    season_id,
    region_type,
    region_ref_id,
    rank_no,
    total_score
  );

ALTER TABLE place_ranking_history
  ADD INDEX idx_place_ranking_history_place_season_region_rank (
    place_id,
    season_id,
    region_type,
    region_ref_id,
    rank_no
  );

ALTER TABLE place_ranking_history
  ADD INDEX idx_place_ranking_history_season_region_rank (
    season_id,
    region_type,
    region_ref_id,
    rank_no
  );
