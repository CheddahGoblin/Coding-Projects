// Shared PostgreSQL (Supabase) connection pool.
require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const { Pool } = require('pg');

if (!process.env.DATABASE_URL) {
  throw new Error('DATABASE_URL is missing. Copy .env.example to .env and fill it in.');
}

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }, // Supabase requires SSL
  max: 10,
});

// Keeps an idle-connection hiccup from crashing the server.
pool.on('error', (err) => console.error('Unexpected database error:', err.message));

module.exports = pool;