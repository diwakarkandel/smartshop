CREATE TABLE refresh_token_blacklist (
    id uuid PRIMARY KEY,
    token_jti varchar(500) NOT NULL UNIQUE,
    expires_at timestamp NOT NULL,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_blacklist_jti ON refresh_token_blacklist (token_jti);
CREATE INDEX idx_refresh_token_blacklist_expires_at ON refresh_token_blacklist (expires_at);
