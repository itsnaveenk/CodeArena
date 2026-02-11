import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { validateEmail, validatePassword, validateRequired } from './validations';

describe('Property 1: Email Validation Correctness', () => {
  const validEmailArbitrary = fc.tuple(
    fc.string({ minLength: 1, maxLength: 20 }).filter(s => /^[a-zA-Z][a-zA-Z0-9]*$/.test(s)),
    fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-zA-Z][a-zA-Z0-9]*$/.test(s)),
    fc.constantFrom('com', 'org', 'net', 'io', 'co')
  ).map(([local, domain, tld]) => `${local}@${domain}.${tld}`);

  it('should accept valid email formats', () => {
    fc.assert(
      fc.property(
        validEmailArbitrary,
        (email) => {
          expect(validateEmail(email)).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should reject strings without @ symbol', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1 }).filter(s => !s.includes('@')),
        (str) => {
          expect(validateEmail(str)).toBe(false);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should reject empty strings', () => {
    expect(validateEmail('')).toBe(false);
  });

  it('should reject strings with multiple @ symbols', () => {
    fc.assert(
      fc.property(
        fc.tuple(
          fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s)),
          fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s)),
          fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s))
        ).map(([a, b, c]) => `${a}@${b}@${c}`),
        (str) => {
          expect(validateEmail(str)).toBe(false);
        }
      ),
      { numRuns: 100 }
    );
  });
});

describe('Property 2: Password Validation Correctness', () => {
  it('should accept passwords with 8 or more characters', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 8, maxLength: 100 }),
        (password) => {
          expect(validatePassword(password)).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should reject passwords with fewer than 8 characters', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 0, maxLength: 7 }),
        (password) => {
          expect(validatePassword(password)).toBe(false);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept exactly 8 character passwords', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 8, maxLength: 8 }),
        (password) => {
          expect(validatePassword(password)).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });
});

describe('Property 3: Required Field Validation Correctness', () => {
  it('should accept non-empty, non-whitespace strings', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1 }).filter(s => s.trim().length > 0),
        (str) => {
          expect(validateRequired(str)).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should reject empty strings', () => {
    expect(validateRequired('')).toBe(false);
  });

  it('should reject whitespace-only strings', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(' ', '  ', '\t', '\n', '\r', '   ', '\t\t', ' \t \n'),
        (whitespace) => {
          expect(validateRequired(whitespace)).toBe(false);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept strings with leading/trailing whitespace but non-whitespace content', () => {
    fc.assert(
      fc.property(
        fc.tuple(
          fc.constantFrom('', ' ', '  ', '\t'),
          fc.string({ minLength: 1 }).filter(s => s.trim().length > 0),
          fc.constantFrom('', ' ', '  ', '\t')
        ).map(([pre, content, post]) => `${pre}${content}${post}`),
        (str) => {
          expect(validateRequired(str)).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });
});
