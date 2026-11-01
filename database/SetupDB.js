// Connects to Supabase Postgres, runs schema.sql, and prints a quick check.
require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { Client } = require('pg');

async function main() {
  const client = new Client({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }, // Supabase requires SSL
  });

  await client.connect();
  console.log('Connected to', (await client.query('SELECT current_database() AS db')).rows[0].db);

  const sql = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
  await client.query(sql); // runs the whole script
  console.log('schema.sql applied');

  const { rows } = await client.query(`
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
    ORDER BY table_name`);
  console.log('Tables:', rows.map(r => r.table_name).join(', '));

  const users = await client.query('SELECT id, email, role FROM users ORDER BY id');
  console.table(users.rows);

  await client.end();
}

main().catch(err => {
  console.error('Setup failed:', err.message);
  process.exit(1);
});