// Creates a throwaway in-memory PostgreSQL database (pg-mem) using the project's real schema.
// Each call returns a brand-new, empty database, so tests are repeatable and never touch Supabase.
const fs = require('fs');
const path = require('path');
const { newDb } = require('pg-mem');

const SCHEMA_PATH = path.join(__dirname, '..', '..', '..', 'database', 'Schema.sql');

// Only the table and index definitions are loaded. The Supabase-specific
// row-level-security statements and sample data come after this marker.
const loadTableDefinitions = () =>
  fs.readFileSync(SCHEMA_PATH, 'utf8').split('-- ---------- Supabase')[0];

const createTestPool = () => {
  const db = newDb();
  db.public.none(loadTableDefinitions());
  const { Pool } = db.adapters.createPg();
  return new Pool();
};

module.exports = { createTestPool };
