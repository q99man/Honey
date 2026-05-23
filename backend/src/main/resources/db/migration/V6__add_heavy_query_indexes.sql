-- Optimize User Action Logs queries (sorting by created_at DESC)
ALTER TABLE user_action_logs
  ADD INDEX idx_user_action_logs_created_at (created_at DESC);

-- Optimize Admin Action Logs queries (sorting by created_at DESC)
ALTER TABLE admin_action_logs
  ADD INDEX idx_admin_action_logs_created_at (created_at DESC);

-- Optimize Active Sanction validation check
ALTER TABLE user_sanctions
  ADD INDEX idx_user_sanctions_active_check (user_id, status, sanction_type, start_at, end_at);

-- Optimize Demographics aggregation queries (covering index for visit subquery)
ALTER TABLE visits
  ADD INDEX idx_visits_place_valid_user (place_id, is_valid, user_id);

-- Optimize User list sorting queries (sorting by created_at DESC)
ALTER TABLE users
  ADD INDEX idx_users_created_at (created_at DESC);
