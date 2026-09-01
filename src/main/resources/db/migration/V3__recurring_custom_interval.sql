ALTER TABLE recurring_transactions
    ADD COLUMN IF NOT EXISTS custom_interval_days INTEGER NOT NULL DEFAULT 1;
