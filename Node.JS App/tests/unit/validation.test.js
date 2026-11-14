// Unit tests for lib/validation.js (pure functions, no dependencies to replace).
const { describe, test } = require('node:test');
const assert = require('node:assert/strict');
const { escapeHtml, validateRegistration } = require('../../lib/validation');

// ---------------------------------------------------------------------------
// Unit 1: escapeHtml
// ---------------------------------------------------------------------------
describe('Unit: escapeHtml', () => {
  test('expected: escapes each of the five HTML special characters', () => {
    assert.equal(escapeHtml(`& < > " '`), '&amp; &lt; &gt; &quot; &#39;');
  });

  test('expected: neutralizes a script-tag injection attempt', () => {
    assert.equal(
      escapeHtml('<script>alert("x")</script>'),
      '&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;'
    );
  });

  test('edge: empty string and plain text are returned unchanged', () => {
    assert.equal(escapeHtml(''), '');
    assert.equal(escapeHtml("Sam O Bass"), 'Sam O Bass');
  });

  test('unusual input: non-string values are converted to text instead of throwing', () => {
    assert.equal(escapeHtml(42), '42');
    assert.equal(escapeHtml(null), 'null');
    assert.equal(escapeHtml(undefined), 'undefined');
  });
});

// ---------------------------------------------------------------------------
// Unit 2: validateRegistration
// ---------------------------------------------------------------------------
const validForm = () => ({
  fullName: 'Jane Customer',
  email: 'jane@example.com',
  password: 'password123',
  confirmPassword: 'password123',
});

describe('Unit: validateRegistration', () => {
  test('expected: valid form returns trimmed name and lower-cased email', () => {
    const result = validateRegistration({
      ...validForm(),
      fullName: '  Jane Customer  ',
      email: '  Jane@Example.COM ',
    });
    assert.deepEqual(result, {
      value: { fullName: 'Jane Customer', email: 'jane@example.com', password: 'password123' },
    });
  });

  test('boundary: password of exactly 8 characters passes, 7 characters is "weak"', () => {
    const eight = validateRegistration({ ...validForm(), password: '12345678', confirmPassword: '12345678' });
    const seven = validateRegistration({ ...validForm(), password: '1234567', confirmPassword: '1234567' });
    assert.equal(eight.error, undefined);
    assert.deepEqual(seven, { error: 'weak' });
  });

  test('boundary: password limit is 72 bytes, not 72 characters', () => {
    const ascii72 = 'a'.repeat(72);
    const multiByte = 'é'.repeat(37); // 37 characters but 74 bytes in UTF-8
    assert.equal(validateRegistration({ ...validForm(), password: ascii72, confirmPassword: ascii72 }).error, undefined);
    assert.deepEqual(
      validateRegistration({ ...validForm(), password: multiByte, confirmPassword: multiByte }),
      { error: 'weak' }
    );
  });

  test('boundary: full name of 100 characters passes, 101 is "invalid"', () => {
    assert.equal(validateRegistration({ ...validForm(), fullName: 'x'.repeat(100) }).error, undefined);
    assert.deepEqual(validateRegistration({ ...validForm(), fullName: 'x'.repeat(101) }), { error: 'invalid' });
  });

  test('invalid: malformed email and an empty form are both rejected as "invalid"', () => {
    assert.deepEqual(validateRegistration({ ...validForm(), email: 'not-an-email' }), { error: 'invalid' });
    assert.deepEqual(validateRegistration({}), { error: 'invalid' });
    assert.deepEqual(validateRegistration(undefined), { error: 'invalid' });
  });

  test('invalid: confirmation that does not match the password returns "mismatch"', () => {
    assert.deepEqual(
      validateRegistration({ ...validForm(), confirmPassword: 'password124' }),
      { error: 'mismatch' }
    );
  });

  test('invalid: a non-string password (e.g. an array from a tampered form) is "weak"', () => {
    const arr = ['password123', 'password123'];
    assert.deepEqual(validateRegistration({ ...validForm(), password: arr, confirmPassword: arr }), { error: 'weak' });
  });
});
