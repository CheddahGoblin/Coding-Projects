// Unit tests for lib/auth.js. The database is replaced with a fake pool,
// and Express's req/res/next are replaced with plain objects.
const { describe, test } = require('node:test');
const assert = require('node:assert/strict');
const bcrypt = require('bcryptjs');
const { authenticateUser, requireAuth } = require('../../lib/auth');

// ---------------------------------------------------------------------------
// Unit 3: requireAuth (middleware)
// ---------------------------------------------------------------------------
// Fake Express response that records where it redirected.
const fakeRes = () => {
  const res = { redirectedTo: null };
  res.redirect = (url) => { res.redirectedTo = url; };
  return res;
};

describe('Unit: requireAuth', () => {
  test('expected: logged-in session calls next() and does not redirect', () => {
    const req = { session: { user: { id: 1, fullName: 'Jane' } } };
    const res = fakeRes();
    let nextCalls = 0;

    requireAuth(req, res, () => { nextCalls += 1; });

    assert.equal(nextCalls, 1);
    assert.equal(res.redirectedTo, null);
  });

  test('invalid: session without a user is redirected to the login page', () => {
    const res = fakeRes();
    let nextCalls = 0;

    requireAuth({ session: {} }, res, () => { nextCalls += 1; });

    assert.equal(nextCalls, 0);
    assert.equal(res.redirectedTo, '/');
  });

  test('edge: request with no session object at all is redirected, not crashed', () => {
    const res = fakeRes();
    let nextCalls = 0;

    assert.doesNotThrow(() => requireAuth({}, res, () => { nextCalls += 1; }));
    assert.equal(nextCalls, 0);
    assert.equal(res.redirectedTo, '/');
  });
});

// ---------------------------------------------------------------------------
// Unit 4: authenticateUser
// ---------------------------------------------------------------------------
// Low cost factor keeps the test fast; bcrypt.compare reads the cost from the hash.
const STORED_HASH = bcrypt.hashSync('correct-horse', 4);
const STORED_USER = {
  id: 7, email: 'jane@example.com', password_hash: STORED_HASH, full_name: 'Jane', role: 'customer',
};

// Fake pool: returns the given rows (or throws) and records every query it receives.
const fakePool = ({ rows = [], error = null } = {}) => ({
  calls: [],
  async query(sql, params) {
    this.calls.push({ sql, params });
    if (error) throw error;
    return { rows };
  },
});

describe('Unit: authenticateUser', () => {
  test('expected: correct password returns the user and queries with the trimmed email', async () => {
    const pool = fakePool({ rows: [STORED_USER] });

    const user = await authenticateUser(pool, '  Jane@Example.com ', 'correct-horse');

    assert.deepEqual(user, STORED_USER);
    assert.equal(pool.calls.length, 1);
    assert.deepEqual(pool.calls[0].params, ['Jane@Example.com']);
    assert.match(pool.calls[0].sql, /lower\(email\) = lower\(\$1\)/); // case-insensitive, parameterized
  });

  test('invalid: wrong password returns null', async () => {
    const pool = fakePool({ rows: [STORED_USER] });
    assert.equal(await authenticateUser(pool, 'jane@example.com', 'wrong-password'), null);
  });

  test('invalid: unknown email returns null', async () => {
    const pool = fakePool({ rows: [] });
    assert.equal(await authenticateUser(pool, 'nobody@example.com', 'correct-horse'), null);
    assert.equal(pool.calls.length, 1);
  });

  test('edge: blank email or non-string password returns null without touching the database', async () => {
    const pool = fakePool({ rows: [STORED_USER] });

    assert.equal(await authenticateUser(pool, '   ', 'correct-horse'), null);
    assert.equal(await authenticateUser(pool, 'jane@example.com', ''), null);
    assert.equal(await authenticateUser(pool, 'jane@example.com', ['correct-horse']), null);
    assert.equal(pool.calls.length, 0);
  });

  test('error: a database failure is passed up to the caller', async () => {
    const pool = fakePool({ error: new Error('connection refused') });
    await assert.rejects(
      authenticateUser(pool, 'jane@example.com', 'correct-horse'),
      { message: 'connection refused' }
    );
  });
});
