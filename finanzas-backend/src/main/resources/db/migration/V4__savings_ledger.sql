CREATE TABLE savings_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    allocation_mode VARCHAR(30) NOT NULL DEFAULT 'PERCENTAGE',
    percentage NUMERIC(5, 2) NOT NULL DEFAULT 20.00,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT savings_config_workspace_unique UNIQUE (workspace_id),
    CONSTRAINT savings_config_mode_valid CHECK (allocation_mode IN ('PERCENTAGE')),
    CONSTRAINT savings_config_percentage_valid CHECK (percentage >= 0 AND percentage <= 100)
);

CREATE TABLE savings_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    source_transaction_id UUID,
    goal_id UUID REFERENCES savings_goals(id) ON DELETE SET NULL,
    type VARCHAR(40) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    effective_date DATE NOT NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    idempotency_key VARCHAR(160) NOT NULL,
    reversed_movement_id UUID REFERENCES savings_movements(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT savings_movement_type_valid CHECK (type IN ('AUTO_ALLOCATION', 'MANUAL_DEPOSIT', 'WITHDRAWAL', 'ADJUSTMENT', 'REVERSAL', 'GOAL_ALLOCATION', 'GOAL_RELEASE')),
    CONSTRAINT savings_movement_direction_valid CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT savings_movement_amount_positive CHECK (amount > 0),
    CONSTRAINT savings_movement_idempotency_unique UNIQUE (workspace_id, idempotency_key)
);

CREATE INDEX idx_savings_config_workspace ON savings_config(workspace_id);
CREATE INDEX idx_savings_movements_workspace_date ON savings_movements(workspace_id, effective_date DESC, created_at DESC);
CREATE INDEX idx_savings_movements_source_transaction ON savings_movements(workspace_id, source_transaction_id);
CREATE INDEX idx_savings_movements_goal ON savings_movements(workspace_id, goal_id);
