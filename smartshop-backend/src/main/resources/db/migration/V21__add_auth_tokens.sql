-- V21: Add email verification and password reset tokens

ALTER TABLE users ADD COLUMN email_verified boolean NOT NULL DEFAULT false;
UPDATE users SET email_verified = true;

CREATE TABLE password_reset_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token varchar(255) NOT NULL UNIQUE,
    expires_at timestamp NOT NULL,
    used boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE email_verification_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token varchar(255) NOT NULL UNIQUE,
    expires_at timestamp NOT NULL,
    used boolean NOT NULL DEFAULT false,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token);
