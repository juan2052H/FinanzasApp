ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

CREATE TABLE account_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT account_tokens_type_valid CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_account_tokens_user_type_active
    ON account_tokens(user_id, type, consumed_at);

CREATE INDEX idx_account_tokens_expires_at
    ON account_tokens(expires_at);
