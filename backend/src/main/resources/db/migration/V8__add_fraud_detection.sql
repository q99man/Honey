CREATE TABLE fraud_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  alert_type VARCHAR(50) NOT NULL,
  risk_score DOUBLE NOT NULL,
  description VARCHAR(255) NOT NULL,
  ip_address VARCHAR(64),
  target_id BIGINT,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_fraud_alerts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_fraud_alerts_created ON fraud_alerts (created_at DESC);
CREATE INDEX idx_fraud_alerts_user_type ON fraud_alerts (user_id, alert_type);
