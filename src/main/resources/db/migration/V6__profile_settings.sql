ALTER TABLE users
    ADD COLUMN ciudad VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN pais VARCHAR(120) NOT NULL DEFAULT '';

ALTER TABLE user_settings
    ADD COLUMN locale VARCHAR(20) NOT NULL DEFAULT 'es-CO',
    ADD COLUMN time_zone VARCHAR(80) NOT NULL DEFAULT 'America/Bogota',
    ADD COLUMN money_format VARCHAR(40) NOT NULL DEFAULT 'SYMBOL_GROUP_DECIMAL',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE user_settings settings
SET locale = users.locale
FROM users
WHERE settings.user_id = users.id;

ALTER TABLE user_settings
    ADD CONSTRAINT user_settings_money_format_valid
    CHECK (money_format IN ('SYMBOL_GROUP_DECIMAL', 'SYMBOL_COMMA_DECIMAL', 'CODE_SUFFIX'));
