ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE TABLE email_verification_tokens (
                                           id         UUID PRIMARY KEY,
                                           user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           token_hash VARCHAR(64) NOT NULL UNIQUE,
                                           issued_at  TIMESTAMP   NOT NULL,
                                           expires_at TIMESTAMP   NOT NULL,
                                           created_at TIMESTAMP,
                                           updated_at TIMESTAMP
);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);