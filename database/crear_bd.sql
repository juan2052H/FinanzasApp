-- =====================================================================
-- FinanzasApp - crear_bd.sql (PostgreSQL 16)
-- Generado por scripts/generate-crear-bd.ps1 a partir de las migraciones
-- Flyway V1..V9 y database/datos_prueba.sql. No editar a mano.
--
-- Uso (base vacia ya creada):
--   psql -h localhost -p 5432 -U finanzas -d finanzas -v ON_ERROR_STOP=1 -f database/crear_bd.sql
-- Si despues se arranca el backend sobre esta base, definir:
--   SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
--   SPRING_FLYWAY_BASELINE_VERSION=9
-- =====================================================================

-- ---------------------------------------------------------------------
-- V1__initial_finanzas_schema.sql
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- V2__persistent_refresh_tokens.sql
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, revoked_at);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ---------------------------------------------------------------------
-- V3__recurring_custom_interval.sql
-- ---------------------------------------------------------------------
ALTER TABLE recurring_transactions
    ADD COLUMN IF NOT EXISTS custom_interval_days INTEGER NOT NULL DEFAULT 1;

-- ---------------------------------------------------------------------
-- V4__savings_ledger.sql
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- V5__auth_lifecycle_tokens.sql
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- V6__profile_settings.sql
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- V7__account_deletion.sql
-- ---------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- ---------------------------------------------------------------------
-- V8__invoices_and_tax.sql
-- ---------------------------------------------------------------------
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    transaction_id UUID REFERENCES transactions(id) ON DELETE SET NULL,
    invoice_number VARCHAR(120) NOT NULL,
    merchant_name VARCHAR(160) NOT NULL,
    tax_id VARCHAR(60) NOT NULL DEFAULT '',
    issue_date DATE NOT NULL,
    subtotal NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(19, 2) NOT NULL,
    attachment_ref VARCHAR(1024) NOT NULL DEFAULT '',
    notes VARCHAR(500) NOT NULL DEFAULT '',
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT invoices_total_positive CHECK (total_amount > 0),
    CONSTRAINT invoices_subtotal_non_negative CHECK (subtotal >= 0),
    CONSTRAINT invoices_tax_non_negative CHECK (tax_amount >= 0)
);

CREATE TABLE tax_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    tax_year INT NOT NULL,
    uvt_value NUMERIC(19, 2) NOT NULL,
    gross_income_uvt INT NOT NULL DEFAULT 1400,
    gross_purchases_uvt INT NOT NULL DEFAULT 1400,
    bank_deposits_uvt INT NOT NULL DEFAULT 1400,
    gross_wealth_uvt INT NOT NULL DEFAULT 4500,
    estimated_gross_wealth NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT tax_config_year_unique UNIQUE (workspace_id, tax_year)
);

CREATE INDEX idx_invoices_workspace_date ON invoices(workspace_id, issue_date DESC);
CREATE INDEX idx_invoices_transaction ON invoices(transaction_id);
CREATE INDEX idx_tax_configurations_workspace ON tax_configurations(workspace_id, tax_year);

-- ---------------------------------------------------------------------
-- V9__users_moneda_varchar.sql
-- ---------------------------------------------------------------------
ALTER TABLE users
    ALTER COLUMN moneda TYPE VARCHAR(3);

-- =====================================================================
-- Datos de prueba
-- =====================================================================
-- =====================================================================
-- FinanzasApp - Datos de prueba (PostgreSQL 16)
--
-- Requiere que el esquema ya exista (migraciones Flyway V1..V8 aplicadas
-- por el backend, o el script crear_bd.sql).
-- Es idempotente: se puede ejecutar varias veces sin duplicar datos.
--
-- Usuarios de prueba (todos con la contrasena: Prueba2026*)
--   ana.duena@finanzasapp.test      OWNER del hogar "Hogar Gomez"
--   carlos.miembro@finanzasapp.test MEMBER del hogar
--   laura.lectora@finanzasapp.test  VIEWER del hogar (solo lectura)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- ---------------------------------------------------------------------
-- Usuarios (hash BCrypt $2a$, costo 12, compatible con BCryptPasswordEncoder)
-- ---------------------------------------------------------------------
INSERT INTO users (id, nombre, apellido, email, password_hash, moneda, locale, tipo_cuenta,
                   proveedor_autenticacion, email_verified_at, ciudad, pais)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Ana', 'Gomez', 'ana.duena@finanzasapp.test',
     crypt('Prueba2026*', gen_salt('bf', 12)), 'COP', 'es-CO', 'HOUSEHOLD', 'PASSWORD', now(), 'Medellin', 'Colombia'),
    ('10000000-0000-0000-0000-000000000002', 'Carlos', 'Ruiz', 'carlos.miembro@finanzasapp.test',
     crypt('Prueba2026*', gen_salt('bf', 12)), 'COP', 'es-CO', 'PERSONAL', 'PASSWORD', now(), 'Medellin', 'Colombia'),
    ('10000000-0000-0000-0000-000000000003', 'Laura', 'Mejia', 'laura.lectora@finanzasapp.test',
     crypt('Prueba2026*', gen_salt('bf', 12)), 'COP', 'es-CO', 'PERSONAL', 'PASSWORD', now(), 'Bogota', 'Colombia')
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_settings (user_id, theme, locale, time_zone, money_format)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'LIGHT', 'es-CO', 'America/Bogota', 'SYMBOL_GROUP_DECIMAL'),
    ('10000000-0000-0000-0000-000000000002', 'LIGHT', 'es-CO', 'America/Bogota', 'SYMBOL_GROUP_DECIMAL'),
    ('10000000-0000-0000-0000-000000000003', 'DARK',  'es-CO', 'America/Bogota', 'SYMBOL_GROUP_DECIMAL')
ON CONFLICT (user_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Workspaces: uno personal por usuario y un hogar compartido
-- ---------------------------------------------------------------------
INSERT INTO workspaces (id, nombre, tipo, owner_id)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'Mi espacio',  'PERSONAL',  '10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002', 'Mi espacio',  'PERSONAL',  '10000000-0000-0000-0000-000000000002'),
    ('20000000-0000-0000-0000-000000000003', 'Mi espacio',  'PERSONAL',  '10000000-0000-0000-0000-000000000003'),
    ('20000000-0000-0000-0000-000000000010', 'Hogar Gomez', 'HOUSEHOLD', '10000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO workspace_members (workspace_id, user_id, role)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'OWNER'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'OWNER'),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'OWNER'),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'OWNER'),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000002', 'MEMBER'),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000003', 'VIEWER')
ON CONFLICT (workspace_id, user_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Categorias por defecto (mismas que DefaultCategoryService) en cada workspace
-- ---------------------------------------------------------------------
INSERT INTO categories (workspace_id, nombre, tipo, icono, color)
SELECT w.id, c.nombre, c.tipo, c.icono, c.color
FROM workspaces w
CROSS JOIN (VALUES
    ('Salario',         'INCOME',  'ING', '#34a853'),
    ('Freelance',       'INCOME',  'FRE', '#1a73e8'),
    ('Negocio',         'INCOME',  'NEG', '#1a73e8'),
    ('Otros',           'INCOME',  'OTR', '#64748b'),
    ('Alimentacion',    'EXPENSE', 'ALI', '#f59e0b'),
    ('Vivienda',        'EXPENSE', 'VIV', '#1a73e8'),
    ('Transporte',      'EXPENSE', 'TRA', '#6366f1'),
    ('Salud',           'EXPENSE', 'SAL', '#ef4444'),
    ('Educacion',       'EXPENSE', 'EDU', '#14b8a6'),
    ('Entretenimiento', 'EXPENSE', 'ENT', '#8b5cf6'),
    ('Servicios',       'EXPENSE', 'SER', '#0ea5e9'),
    ('Suscripciones',   'EXPENSE', 'SUB', '#475569'),
    ('Deudas',          'EXPENSE', 'DEU', '#b91c1c'),
    ('Otros',           'EXPENSE', 'OTR', '#64748b')
) AS c(nombre, tipo, icono, color)
WHERE w.id IN ('20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
               '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000010')
ON CONFLICT (workspace_id, nombre, tipo) DO NOTHING;

-- ---------------------------------------------------------------------
-- Transacciones del hogar: mes anterior y mes actual (fechas relativas a hoy)
-- ---------------------------------------------------------------------
INSERT INTO transactions (id, workspace_id, category_id, created_by_user_id, tipo, descripcion, monto, transaction_date)
SELECT t.id::uuid, '20000000-0000-0000-0000-000000000010', c.id, t.usuario::uuid, t.tipo, t.descripcion, t.monto,
       (date_trunc('month', CURRENT_DATE) + t.desfase)::date
FROM (VALUES
    -- mes anterior
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'INCOME',  'Salario',         'Salario Ana',              4500000.00, interval '-1 month'),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'INCOME',  'Freelance',       'Proyecto web Carlos',      1200000.00, interval '-1 month' + interval '9 days'),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'EXPENSE', 'Vivienda',        'Arriendo apartamento',     1500000.00, interval '-1 month' + interval '1 day'),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'EXPENSE', 'Alimentacion',    'Mercado quincenal',         420000.00, interval '-1 month' + interval '4 days'),
    ('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'EXPENSE', 'Servicios',       'Energia, agua e internet',  320000.00, interval '-1 month' + interval '11 days'),
    ('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000002', 'EXPENSE', 'Transporte',      'Recarga transporte',        180000.00, interval '-1 month' + interval '14 days'),
    ('30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'EXPENSE', 'Entretenimiento', 'Cine y comida',              95000.00, interval '-1 month' + interval '20 days'),
    -- mes actual (dia 1 del mes, para no generar fechas futuras)
    ('30000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'INCOME',  'Salario',         'Salario Ana',              4500000.00, interval '0 days'),
    ('30000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'EXPENSE', 'Vivienda',        'Arriendo apartamento',     1500000.00, interval '0 days'),
    ('30000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000002', 'EXPENSE', 'Alimentacion',    'Mercado del mes',           650000.00, interval '0 days'),
    ('30000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'EXPENSE', 'Entretenimiento', 'Concierto',                 120000.00, interval '0 days')
) AS t(id, usuario, tipo, categoria, descripcion, monto, desfase)
JOIN categories c
  ON c.workspace_id = '20000000-0000-0000-0000-000000000010' AND c.nombre = t.categoria AND c.tipo = t.tipo
ON CONFLICT (id) DO NOTHING;

-- Algunos movimientos personales de Carlos
INSERT INTO transactions (id, workspace_id, category_id, created_by_user_id, tipo, descripcion, monto, transaction_date)
SELECT t.id::uuid, '20000000-0000-0000-0000-000000000002', c.id, '10000000-0000-0000-0000-000000000002', t.tipo, t.descripcion, t.monto,
       date_trunc('month', CURRENT_DATE)::date
FROM (VALUES
    ('30000000-0000-0000-0000-000000000021', 'INCOME',  'Salario',       'Salario Carlos',  2800000.00),
    ('30000000-0000-0000-0000-000000000022', 'EXPENSE', 'Suscripciones', 'Streaming',         45000.00)
) AS t(id, tipo, categoria, descripcion, monto)
JOIN categories c
  ON c.workspace_id = '20000000-0000-0000-0000-000000000002' AND c.nombre = t.categoria AND c.tipo = t.tipo
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Ahorro: configuracion 20% y asignaciones automaticas por cada ingreso
-- (misma clave de idempotencia que usa SavingsService)
-- ---------------------------------------------------------------------
INSERT INTO savings_config (workspace_id, enabled, allocation_mode, percentage, effective_from, created_by_user_id)
VALUES
    ('20000000-0000-0000-0000-000000000010', true, 'PERCENTAGE', 20.00, (date_trunc('month', CURRENT_DATE) - interval '1 month')::date, '10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002', true, 'PERCENTAGE', 10.00, date_trunc('month', CURRENT_DATE)::date, '10000000-0000-0000-0000-000000000002')
ON CONFLICT (workspace_id) DO NOTHING;

INSERT INTO savings_movements (workspace_id, created_by_user_id, source_transaction_id, type, direction, amount,
                               effective_date, note, idempotency_key)
SELECT t.workspace_id, t.created_by_user_id, t.id, 'AUTO_ALLOCATION', 'CREDIT',
       round(t.monto * cfg.percentage / 100, 2), t.transaction_date, 'Ahorro automatico por ingreso',
       'transaction:' || t.id || ':auto-allocation'
FROM transactions t
JOIN savings_config cfg ON cfg.workspace_id = t.workspace_id AND cfg.enabled
WHERE t.tipo = 'INCOME'
  AND t.id::text LIKE '30000000-0000-0000-0000-%'
ON CONFLICT (workspace_id, idempotency_key) DO NOTHING;

-- ---------------------------------------------------------------------
-- Presupuestos del mes actual (Entretenimiento queda excedido a proposito)
-- ---------------------------------------------------------------------
INSERT INTO budgets (workspace_id, category_id, period_month, monto_presupuestado)
SELECT '20000000-0000-0000-0000-000000000010', c.id, date_trunc('month', CURRENT_DATE)::date, b.monto
FROM (VALUES
    ('Alimentacion',     800000.00),
    ('Transporte',       250000.00),
    ('Entretenimiento',  100000.00),
    ('Vivienda',        1500000.00)
) AS b(categoria, monto)
JOIN categories c
  ON c.workspace_id = '20000000-0000-0000-0000-000000000010' AND c.nombre = b.categoria AND c.tipo = 'EXPENSE'
ON CONFLICT (workspace_id, category_id, period_month) DO NOTHING;

-- ---------------------------------------------------------------------
-- Metas de ahorro
-- ---------------------------------------------------------------------
INSERT INTO savings_goals (id, workspace_id, nombre, monto_actual, monto_objetivo, color, icono, due_date, status)
VALUES
    ('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', 'Fondo de emergencia', 0.00, 10000000.00, '#14b8a6', 'META', (CURRENT_DATE + interval '10 months')::date, 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000010', 'Vacaciones en familia', 0.00, 3000000.00, '#f59e0b', 'META', (CURRENT_DATE + interval '6 months')::date, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Movimiento recurrente: arriendo mensual
-- ---------------------------------------------------------------------
INSERT INTO recurring_transactions (id, workspace_id, category_id, tipo, descripcion, monto, frequency, next_run_date, active)
SELECT '50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', c.id, 'EXPENSE',
       'Arriendo apartamento', 1500000.00, 'MONTHLY', (date_trunc('month', CURRENT_DATE) + interval '1 month')::date, true
FROM categories c
WHERE c.workspace_id = '20000000-0000-0000-0000-000000000010' AND c.nombre = 'Vivienda' AND c.tipo = 'EXPENSE'
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Gasto compartido: mercado pagado por Ana, dividido en partes iguales
-- ---------------------------------------------------------------------
INSERT INTO shared_expenses (id, workspace_id, paid_by_user_id, category_id, descripcion, monto, expense_date, split_method, status)
SELECT '60000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001',
       c.id, 'Mercado compartido', 300000.00, date_trunc('month', CURRENT_DATE)::date, 'EQUAL', 'OPEN'
FROM categories c
WHERE c.workspace_id = '20000000-0000-0000-0000-000000000010' AND c.nombre = 'Alimentacion' AND c.tipo = 'EXPENSE'
ON CONFLICT (id) DO NOTHING;

INSERT INTO expense_splits (id, shared_expense_id, user_id, amount, percentage, settled_amount)
VALUES
    ('61000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 150000.00, 50.0000, 0.00),
    ('61000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 150000.00, 50.0000, 0.00)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Factura asociada al pago de servicios del mes anterior
-- ---------------------------------------------------------------------
INSERT INTO invoices (id, workspace_id, transaction_id, invoice_number, merchant_name, tax_id, issue_date,
                      subtotal, tax_amount, total_amount, notes, created_by_user_id)
SELECT '70000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', t.id, 'SPD-2026-000123',
       'Servicios Publicos Demo S.A.S.', '900000000-1', t.transaction_date, 268907.56, 51092.44, 320000.00,
       'Factura de prueba', '10000000-0000-0000-0000-000000000001'
FROM transactions t
WHERE t.id = '30000000-0000-0000-0000-000000000005'
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Notificacion de bienvenida
-- ---------------------------------------------------------------------
INSERT INTO notifications (id, workspace_id, user_id, type, title, body)
VALUES
    ('80000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001',
     'BUDGET_THRESHOLD', 'Presupuesto excedido', 'El presupuesto de Entretenimiento del mes actual fue superado.')
ON CONFLICT (id) DO NOTHING;

COMMIT;
