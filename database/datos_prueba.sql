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
