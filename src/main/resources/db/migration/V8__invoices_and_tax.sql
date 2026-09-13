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
