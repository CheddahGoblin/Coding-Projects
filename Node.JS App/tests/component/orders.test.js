// Component 2: Customer order history.
// Exercises the requireAuth guard + session + /orders route + orders table together.
// Uses a fresh in-memory database built from database/Schema.sql for every test.
const { describe, test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { createApp } = require('../../app');
const { createTestPool } = require('../helpers/testDb');

let pool;
let app;

beforeEach(() => {
  pool = createTestPool();
  app = createApp({ pool, sessionSecret: 'test-secret' });
});

// Registers a customer through the real route and returns a logged-in agent plus their user id.
const registerAndLogin = async (email, fullName = 'Test Customer') => {
  const agent = request.agent(app);
  await agent.post('/register').type('form')
    .send({ fullName, email, password: 'password123', confirmPassword: 'password123' })
    .expect(302).expect('Location', '/dashboard');
  const { rows } = await pool.query('SELECT id FROM users WHERE email = $1', [email]);
  return { agent, userId: rows[0].id };
};

const addOrder = (userId, total, placedAt, status = 'placed') => pool.query(
  'INSERT INTO orders (user_id, total, placed_at, status) VALUES ($1, $2, $3, $4)',
  [userId, total, placedAt, status]
);

describe('Component: customer order history', () => {
  test('success: logged-in customer sees only their own orders, newest first', async () => {
    const jane = await registerAndLogin('jane@example.com');
    const bob = await registerAndLogin('bob@example.com');
    await addOrder(jane.userId, 45.0, '2026-10-01T10:00:00Z', 'delivered');
    await addOrder(bob.userId, 99.99, '2026-10-05T10:00:00Z');
    await addOrder(jane.userId, 83.5, '2026-11-01T10:00:00Z', 'processing');

    const res = await jane.agent.get('/orders').expect(200).expect('Content-Type', /json/);

    assert.equal(res.body.length, 2);
    assert.deepEqual(res.body.map((o) => o.status), ['processing', 'delivered']); // newest first
    assert.deepEqual(res.body.map((o) => Number(o.total)), [83.5, 45]);
    assert.ok(res.body.every((o) => Object.keys(o).sort().join() === 'id,placed_at,status,total'));
  });

  test('edge: a customer with no orders gets an empty list', async () => {
    const { agent } = await registerAndLogin('new@example.com');
    const res = await agent.get('/orders').expect(200);
    assert.deepEqual(res.body, []);
  });

  test('failure: request without a login is redirected and receives no order data', async () => {
    const jane = await registerAndLogin('jane@example.com');
    await addOrder(jane.userId, 45.0, '2026-10-01T10:00:00Z');

    const res = await request(app).get('/orders').expect(302).expect('Location', '/');
    assert.ok(!res.text.includes('45'));
  });

  test('failure: after logout the old session can no longer read orders', async () => {
    const { agent } = await registerAndLogin('jane@example.com');
    await agent.get('/orders').expect(200);

    await agent.get('/logout').expect(302);
    await agent.get('/orders').expect(302).expect('Location', '/');
  });

  test('failure: database error while loading orders returns a 500 error message', async (t) => {
    t.mock.method(console, 'error', () => {});
    const { agent } = await registerAndLogin('jane@example.com');

    // The database starts failing after the user has logged in
    t.mock.method(pool, 'query', async () => { throw new Error('connection lost'); });

    const res = await agent.get('/orders').expect(500);
    assert.equal(res.text, 'Could not load orders.');
  });
});
