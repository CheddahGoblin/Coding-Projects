const express = require('express');
const session = require('express-session');
const http = require('http');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });
const { escapeHtml, validateRegistration } = require('./lib/validation');
const { authenticateUser, createCustomer, loginUser, requireAuth } = require('./lib/auth');

// Builds the Express app. The database pool is passed in so tests can supply
// an in-memory database instead of the live Supabase one.
const createApp = ({ pool, sessionSecret = process.env.SESSION_SECRET }) => {
  if (!sessionSecret) {
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
    secret: sessionSecret,
    resave: false,
    saveUninitialized: false,
    cookie: {
      maxAge: 600000,      // 10 minutes
      httpOnly: true,
      sameSite: 'lax',
      // secure: true,     // turn on once the site is served over HTTPS
    },
  }));

  // POST Route: Process login submissions
  app.post('/login', async (req, res) => {
    const email = req.body.email || req.body.username;
    try {
      const user = await authenticateUser(pool, email, req.body.password);
      if (!user) {
        return res.redirect('/?error=login');
      }
      await loginUser(req, user);
      res.redirect('/dashboard');
    } catch (error) {
      console.error('Login error:', error.message);
      res.redirect('/?error=server');
    }
  });

  // POST Route: Create a new customer account
  app.post('/register', async (req, res) => {
    const result = validateRegistration(req.body);
    if (result.error) {
      return res.redirect(`/?error=${result.error}&form=register`);
    }

    try {
      const user = await createCustomer(pool, result.value);
      await loginUser(req, user);
      res.redirect('/dashboard');
    } catch (error) {
      if (error.code === '23505') { // unique violation: email already registered
        return res.redirect('/?error=exists&form=register');
      }
      console.error('Register error:', error.message);
      res.redirect('/?error=server&form=register');
    }
  });

  // GET Route: Protected Dashboard
  app.get('/dashboard', requireAuth, (req, res) => {
    res.send(`<h1>Welcome, ${escapeHtml(req.session.user.fullName)}!</h1><a href="/logout">Logout</a>`);
  });

  // GET Route: The logged-in customer's orders (query tied to the session user)
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

  return app;
};

module.exports = { createApp };

// Only connect to Supabase and listen when run directly (node app.js), not when imported by tests.
if (require.main === module) {
  const pool = require('./db');
  http.createServer(createApp({ pool })).listen(3000, () => console.log('Server running on http://localhost:3000'));
}
