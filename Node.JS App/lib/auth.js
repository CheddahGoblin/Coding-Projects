// Authentication logic: checking credentials, creating accounts, and session handling.
// Every database function takes the pool as a parameter, so tests can pass in a fake or in-memory database.
const bcrypt = require('bcryptjs');

const BCRYPT_ROUNDS = 10;

// Compared against when the email isn't found, so response time doesn't reveal which emails exist.
const DUMMY_HASH = bcrypt.hashSync('not-a-real-password', BCRYPT_ROUNDS);

// Returns the user row if the email/password pair is valid, otherwise null.
// Database errors are thrown to the caller.
const authenticateUser = async (pool, email, password) => {
  const normalized = String(email || '').trim();
  if (!normalized || typeof password !== 'string' || !password) {
    return null;
  }

  // Parameterized query, safe from SQL injection
  const { rows } = await pool.query(
    `SELECT id, email, password_hash, full_name, role
     FROM users
     WHERE lower(email) = lower($1)`,
    [normalized]
  );
  const user = rows[0];

  const isMatch = await bcrypt.compare(password, user ? user.password_hash : DUMMY_HASH);
  return user && isMatch ? user : null;
};

// Inserts a new customer and returns { id, email, full_name, role }.
// The role is always 'customer'; it is never read from the form.
const createCustomer = async (pool, { fullName, email, password }) => {
  const hash = await bcrypt.hash(password, BCRYPT_ROUNDS);
  const { rows } = await pool.query(
    `INSERT INTO users (email, password_hash, full_name, role)
     VALUES ($1, $2, $3, 'customer')
     RETURNING id, email, full_name, role`,
    [email, hash, fullName]
  );
  return rows[0];
};

// Starts a fresh session (prevents session fixation) and stores who is logged in.
const loginUser = (req, user) => new Promise((resolve, reject) => {
  req.session.regenerate((err) => {
    if (err) return reject(err);
    req.session.user = {
      id: user.id,
      email: user.email,
      fullName: user.full_name,
      role: user.role,
    };
    resolve();
  });
});

// Middleware route guard: protects private routes.
const requireAuth = (req, res, next) => {
  if (req.session && req.session.user) {
    next();
  } else {
    res.redirect('/'); // Bounce unauthenticated users back to the login page
  }
};

module.exports = { BCRYPT_ROUNDS, DUMMY_HASH, authenticateUser, createCustomer, loginUser, requireAuth };
