// Pure input-handling helpers. No database or network access, so they are easy to unit test.

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Escapes user-controlled text before putting it in HTML.
const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));

// Checks the "create account" form.
// Returns { value: { fullName, email, password } } when valid,
// or { error: 'invalid' | 'weak' | 'mismatch' } (the code shown on the landing page).
const validateRegistration = (body = {}) => {
  const fullName = String(body.fullName || '').trim();
  const email = String(body.email || '').trim().toLowerCase();
  const { password, confirmPassword } = body;

  if (!fullName || fullName.length > 100 || !EMAIL_RE.test(email) || email.length > 254) {
    return { error: 'invalid' };
  }
  // bcrypt only uses the first 72 bytes of a password, so reject anything longer
  if (typeof password !== 'string' || password.length < 8 || Buffer.byteLength(password) > 72) {
    return { error: 'weak' };
  }
  if (password !== confirmPassword) {
    return { error: 'mismatch' };
  }
  return { value: { fullName, email, password } };
};

module.exports = { EMAIL_RE, escapeHtml, validateRegistration };
