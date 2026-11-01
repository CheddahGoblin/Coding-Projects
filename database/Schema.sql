-- =====================================================================
-- Order system schema (PostgreSQL / Supabase)
-- Run in the Supabase SQL Editor, or from Node with: node setup-db.js
-- Safe to re-run: tables use IF NOT EXISTS and seed data uses ON CONFLICT.
-- =====================================================================

-- ---------- Users (customers sign in, admins use the JavaFX app) -----
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,                 -- store a bcrypt hash, never the password
    full_name     TEXT NOT NULL,
    role          TEXT NOT NULL DEFAULT 'customer'
                  CHECK (role IN ('customer', 'admin')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Products -------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- Orders (created by the Node.js server) -------------------
CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    status      TEXT NOT NULL DEFAULT 'placed'
                CHECK (status IN ('placed', 'processing', 'shipped', 'delivered', 'cancelled')),
    total       NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    placed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Order line items -----------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    line_no     INT    NOT NULL,
    product_id  BIGINT NOT NULL REFERENCES products(id),
    quantity    INT    NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),  -- price at time of purchase
    PRIMARY KEY (order_id, line_no)
);

-- ---------- Deliveries (loaded by the JavaFX admin app) --------------
CREATE TABLE IF NOT EXISTS deliveries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id       BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    address        TEXT NOT NULL,
    status         TEXT NOT NULL DEFAULT 'scheduled'
                   CHECK (status IN ('scheduled', 'out_for_delivery', 'delivered', 'failed')),
    scheduled_for  DATE,
    delivered_at   TIMESTAMPTZ,
    notes          TEXT
);

-- ---------- Indexes for the common lookups ---------------------------
CREATE INDEX IF NOT EXISTS idx_orders_user      ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_status ON deliveries(status, scheduled_for);

-- ---------- Supabase: lock down the public Data API ------------------
-- Tables in the public schema are reachable through Supabase's REST API.
-- Enabling RLS with no policies blocks that path, while direct Postgres
-- connections from Node.js and JavaFX keep working as before.
ALTER TABLE users       ENABLE ROW LEVEL SECURITY;
ALTER TABLE products    ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders      ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE deliveries  ENABLE ROW LEVEL SECURITY;

-- ---------- Sample data ----------------------------------------------
INSERT INTO users (email, password_hash, full_name, role) VALUES
    ('customer@example.com', 'REPLACE_WITH_BCRYPT_HASH', 'Sample Customer', 'customer'),
    ('admin@example.com',    'REPLACE_WITH_BCRYPT_HASH', 'Sample Admin',    'admin')
ON CONFLICT (email) DO NOTHING;

INSERT INTO products (name, unit_price)
SELECT v.name, v.unit_price
FROM (VALUES ('Mulch (1 cu yd)', 45.00), ('Topsoil (1 cu yd)', 38.50)) AS v(name, unit_price)
WHERE NOT EXISTS (SELECT 1 FROM products);

-- ---------- Optional: limited role for the JavaFX admin app ----------
-- Better than handing every admin machine the main postgres password.
-- Uncomment, set a real password, then connect as  javafx_admin.[PROJECT-REF]
-- CREATE ROLE javafx_admin LOGIN PASSWORD 'change-me';
-- GRANT SELECT ON users, products, orders, order_items TO javafx_admin;
-- GRANT SELECT, UPDATE ON deliveries TO javafx_admin;