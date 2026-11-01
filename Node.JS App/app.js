const express = require('express');
const session = require('express-session');
const http = require('http');
const bcrypt = require('bcryptjs');
const path = require('path');
require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const pool = require('./db');

if (!process.env.SESSION_SECRET) {
  throw new Error('SESSION_SECRET is missing from .env');
}

const app = express();

// Middleware to parse form data and JSON payloads
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

// Configure session management
app.use(session({
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: {
    maxAge: 600000,      // 10 minutes
    httpOnly: true,
    sameSite: 'lax',
    // secure: true,     // turn on once the site is served over HTTPS
  },
}));

// Compared against when the email isn't found, so response time doesn't reveal which emails exist.
const DUMMY_HASH = bcrypt.hashSync('not-a-real-password', 10);

// Escapes user-controlled text before putting it in HTML.
const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

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

// POST Route: Process login submissions
app.post('/login', async (req, res) => {
  const email = String(req.body.email || req.body.username || '').trim();
  const password = req.body.password;

  if (!email || typeof password !== 'string' || !password) {
    return res.redirect('/?error=login');
  }

  try {
    // 1. Look the customer up in Supabase (parameterized query, safe from SQL injection)
    const { rows } = await pool.query(
      `SELECT id, email, password_hash, full_name, role
       FROM users
       WHERE lower(email) = lower($1)`,
      [email]
    );
    const user = rows[0];

    // 2. Compare the submitted password to the stored hash
    const isMatch = await bcrypt.compare(password, user ? user.password_hash : DUMMY_HASH);

    if (!user || !isMatch) {
      return res.redirect('/?error=login');
    }

    // 3. Log them in and send them to the dashboard
    await loginUser(req, user);
    res.redirect('/dashboard');
  } catch (error) {
    console.error('Login error:', error.message);
    res.redirect('/?error=server');
  }
});

// POST Route: Create a new customer account
app.post('/register', async (req, res) => {
  const fullName = String(req.body.fullName || '').trim();
  const email = String(req.body.email || '').trim().toLowerCase();
  const { password, confirmPassword } = req.body;

  if (!fullName || fullName.length > 100 || !EMAIL_RE.test(email) || email.length > 254) {
    return res.redirect('/?error=invalid&form=register');
  }
  // bcrypt only uses the first 72 bytes of a password, so reject anything longer
  if (typeof password !== 'string' || password.length < 8 || Buffer.byteLength(password) > 72) {
    return res.redirect('/?error=weak&form=register');
  }
  if (password !== confirmPassword) {
    return res.redirect('/?error=mismatch&form=register');
  }

  try {
    const hash = await bcrypt.hash(password, 10);

    // The role is always 'customer' here. It is never read from the form.
    const { rows } = await pool.query(
      `INSERT INTO users (email, password_hash, full_name, role)
       VALUES ($1, $2, $3, 'customer')
       RETURNING id, email, full_name, role`,
      [email, hash, fullName]
    );

    await loginUser(req, rows[0]);
    res.redirect('/dashboard');
  } catch (error) {
    if (error.code === '23505') { // unique violation: email already registered
      return res.redirect('/?error=exists&form=register');
    }
    console.error('Register error:', error.message);
    res.redirect('/?error=server&form=register');
  }
});

// Middleware Route Guard: Protects private routes
const requireAuth = (req, res, next) => {
  if (req.session && req.session.user) {
    next();
  } else {
    res.redirect('/'); // Bounce unauthenticated users back to the login page
  }
};

// GET Route: Protected Dashboard
app.get('/dashboard', requireAuth, (req, res) => {
  res.send(`<h1>Welcome, ${escapeHtml(req.session.user.fullName)}!</h1><a href="/logout">Logout</a>`);
});

// GET Route: The logged-in customer's orders (example of a query tied to the session user)
app.get('/orders', requireAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT id, status, total, placed_at FROM orders WHERE user_id = $1 ORDER BY placed_at DESC',
      [req.session.user.id]
    );
    res.json(rows);
  } catch (error) {
    console.error('Orders error:', error.message);
    res.status(500).send('Could not load orders.');
  }
});

// GET Route: Process log outs
app.get('/logout', (req, res) => {
  req.session.destroy(() => {
    res.redirect('/');
  });
});

// Have the app listen at port 3000
http.createServer(app).listen(3000, () => console.log('Server running on http://localhost:3000'));