CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(120) NOT NULL,
    apellido VARCHAR(120) NOT NULL DEFAULT '',
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(512),
    moneda CHAR(3) NOT NULL DEFAULT 'COP',
    locale VARCHAR(20) NOT NULL DEFAULT 'es-CO',
    tipo_cuenta VARCHAR(40) NOT NULL DEFAULT 'PERSONAL',
    avatar_ref VARCHAR(1024),
    proveedor_autenticacion VARCHAR(40) NOT NULL DEFAULT 'PASSWORD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT users_provider_valid CHECK (proveedor_autenticacion IN ('PASSWORD', 'GOOGLE'))
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(160) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT workspaces_tipo_valid CHECK (tipo IN ('PERSONAL', 'HOUSEHOLD', 'BUSINESS'))
);

CREATE TABLE workspace_members (
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT workspace_members_role_valid CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE TABLE workspace_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invited_email VARCHAR(255) NOT NULL,
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT workspace_invitations_status_valid CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT workspace_invitations_role_valid CHECK (role IN ('ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    icono VARCHAR(80) NOT NULL DEFAULT '',
    color VARCHAR(16) NOT NULL DEFAULT '#1a73e8',
    archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT categories_tipo_valid CHECK (tipo IN ('INCOME', 'EXPENSE')),
    UNIQUE (workspace_id, nombre, tipo)
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    tipo VARCHAR(30) NOT NULL,
    descripcion VARCHAR(500) NOT NULL DEFAULT '',
    monto NUMERIC(19, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT transactions_tipo_valid CHECK (tipo IN ('INCOME', 'EXPENSE')),
    CONSTRAINT transactions_monto_positive CHECK (monto > 0)
);

CREATE TABLE budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id),
    period_month DATE NOT NULL,
    monto_presupuestado NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT budgets_amount_non_negative CHECK (monto_presupuestado >= 0),
    UNIQUE (workspace_id, category_id, period_month)
);

CREATE TABLE savings_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    nombre VARCHAR(160) NOT NULL,
    monto_actual NUMERIC(19, 2) NOT NULL DEFAULT 0,
    monto_objetivo NUMERIC(19, 2) NOT NULL,
    color VARCHAR(16) NOT NULL DEFAULT '#1a73e8',
    icono VARCHAR(80) NOT NULL DEFAULT '',
    due_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT savings_goal_amounts_valid CHECK (monto_actual >= 0 AND monto_objetivo > 0),
    CONSTRAINT savings_goal_status_valid CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED'))
);

CREATE TABLE recurring_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id),
    tipo VARCHAR(30) NOT NULL,
    descripcion VARCHAR(500) NOT NULL DEFAULT '',
    monto NUMERIC(19, 2) NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    next_run_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT recurring_tipo_valid CHECK (tipo IN ('INCOME', 'EXPENSE')),
    CONSTRAINT recurring_frequency_valid CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY', 'CUSTOM')),
    CONSTRAINT recurring_amount_positive CHECK (monto > 0)
);

CREATE TABLE shared_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    paid_by_user_id UUID NOT NULL REFERENCES users(id),
    category_id UUID REFERENCES categories(id),
    descripcion VARCHAR(500) NOT NULL,
    monto NUMERIC(19, 2) NOT NULL,
    expense_date DATE NOT NULL,
    split_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT shared_expenses_amount_positive CHECK (monto > 0),
    CONSTRAINT shared_expenses_split_valid CHECK (split_method IN ('EQUAL', 'PERCENTAGE', 'CUSTOM_AMOUNT')),
    CONSTRAINT shared_expenses_status_valid CHECK (status IN ('OPEN', 'SETTLED', 'CANCELLED'))
);

CREATE TABLE expense_splits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shared_expense_id UUID NOT NULL REFERENCES shared_expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(19, 2) NOT NULL,
    percentage NUMERIC(7, 4),
    settled_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT expense_splits_amount_non_negative CHECK (amount >= 0 AND settled_amount >= 0)
);

CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(19, 2) NOT NULL,
    settlement_date DATE NOT NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT settlements_amount_positive CHECK (amount > 0),
    CONSTRAINT settlements_distinct_users CHECK (from_user_id <> to_user_id)
);

CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(30) NOT NULL DEFAULT 'LIGHT',
    notif_presupuesto BOOLEAN NOT NULL DEFAULT true,
    notif_metas BOOLEAN NOT NULL DEFAULT true,
    notif_consejos BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_settings_theme_valid CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM'))
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workspace_members_user ON workspace_members(user_id);
CREATE INDEX idx_invitations_workspace_status ON workspace_invitations(workspace_id, status);
CREATE INDEX idx_categories_workspace ON categories(workspace_id);
CREATE INDEX idx_transactions_workspace_date ON transactions(workspace_id, transaction_date DESC);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_transactions_user ON transactions(created_by_user_id);
CREATE INDEX idx_budgets_workspace_period ON budgets(workspace_id, period_month);
CREATE INDEX idx_goals_workspace_status ON savings_goals(workspace_id, status);
CREATE INDEX idx_recurring_workspace_next_run ON recurring_transactions(workspace_id, next_run_date);
CREATE INDEX idx_shared_expenses_workspace_status ON shared_expenses(workspace_id, status);
CREATE INDEX idx_splits_user ON expense_splits(user_id);
CREATE INDEX idx_settlements_workspace_date ON settlements(workspace_id, settlement_date DESC);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read_at);
CREATE INDEX idx_audit_workspace_created ON audit_logs(workspace_id, created_at DESC);
