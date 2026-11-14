// Component 1: Account registration and login.
// Exercises the Express routes + validation + bcrypt + session + users table together.
// Uses a fresh in-memory database built from database/Schema.sql for every test.
const { describe, test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const bcrypt = require('bcryptjs');
const { createApp } = require('../../app');
const { createTestPool } = require('../helpers/testDb');

const newUser = {
  fullName: 'Jane Customer',
  email: 'jane@example.com',
  password: 'password123',
  confirmPassword: 'password123',
};

let pool;
let agent; // keeps cookies between requests, like a browser

beforeEach(() => {
  pool = createTestPool();
  agent = request.agent(createApp({ pool, sessionSecret: 'test-secret' }));
});

const countUsers = async () => Number((await pool.query('SELECT count(*) AS n FROM users')).rows[0].n);

describe('Component: account registration and login', () => {
  test('success: registering saves a hashed customer and logs them in to the dashboard', async () => {
    await agent.post('/register').type('form').send(newUser)
      .expect(302).expect('Location', '/dashboard');

    // Session works: dashboard greets the new user by name
    const dash = await agent.get('/dashboard').expect(200);
    assert.match(dash.text, /Welcome, Jane Customer!/);

    // Database has exactly one customer row with a real bcrypt hash, not the plain password
    const { rows } = await pool.query('SELECT email, full_name, role, password_hash FROM users');
    assert.equal(rows.length, 1);
    assert.equal(rows[0].email, 'jane@example.com');
    assert.equal(rows[0].role, 'customer');
    assert.notEqual(rows[0].password_hash, 'password123');
    assert.equal(await bcrypt.compare('password123', rows[0].password_hash), true);
  });

  test('success: a registered user can log out and log back in with different email casing', async () => {
    await agent.post('/register').type('form').send(newUser).expect(302);
    await agent.get('/logout').expect(302).expect('Location', '/');

    await agent.post('/login').type('form')
      .send({ email: '  JANE@Example.com ', password: 'password123' })
      .expect(302).expect('Location', '/dashboard');
    await agent.get('/dashboard').expect(200);
  });

  test('failure: wrong password is rejected and no session is created', async () => {
    await pool.query(
      `INSERT INTO users (email, password_hash, full_name) VALUES ($1, $2, $3)`,
      ['jane@example.com', bcrypt.hashSync('password123', 4), 'Jane Customer']
    );

    await agent.post('/login').type('form')
      .send({ email: 'jane@example.com', password: 'not-my-password' })
      .expect(302).expect('Location', '/?error=login');
    await agent.get('/dashboard').expect(302).expect('Location', '/');
  });

  test('failure: registering an email that already exists (any case) is refused, no duplicate row', async () => {
    await agent.post('/register').type('form').send(newUser).expect(302);

    const second = request.agent(createApp({ pool, sessionSecret: 'test-secret' }));
    await second.post('/register').type('form')
      .send({ ...newUser, fullName: 'Impostor', email: 'JANE@example.com' })
      .expect(302).expect('Location', '/?error=exists&form=register');

    assert.equal(await countUsers(), 1);
    await second.get('/dashboard').expect(302).expect('Location', '/'); // impostor not logged in
  });

  test('failure: invalid form data is rejected before anything is written to the database', async () => {
    await agent.post('/register').type('form')
      .send({ ...newUser, confirmPassword: 'different123' })
      .expect(302).expect('Location', '/?error=mismatch&form=register');

    assert.equal(await countUsers(), 0);
    await agent.get('/dashboard').expect(302).expect('Location', '/');
  });

  test('unusual: HTML in a full name is stored as typed but escaped on the dashboard', async () => {
    await agent.post('/register').type('form')
      .send({ ...newUser, fullName: '<script>alert(1)</script>' })
      .expect(302).expect('Location', '/dashboard');

    const dash = await agent.get('/dashboard').expect(200);
    assert.ok(!dash.text.includes('<script>'));
    assert.match(dash.text, /&lt;script&gt;alert\(1\)&lt;\/script&gt;/);
  });

  test('failure: database outage during login sends the user to the server-error message', async (t) => {
    t.mock.method(console, 'error', () => {}); // keep expected error log out of test output
    const brokenPool = { query: async () => { throw new Error('connection refused'); } };
    const app = createApp({ pool: brokenPool, sessionSecret: 'test-secret' });

    await request(app).post('/login').type('form')
      .send({ email: 'jane@example.com', password: 'password123' })
      .expect(302).expect('Location', '/?error=server');
  });
});
