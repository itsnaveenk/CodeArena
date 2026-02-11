import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { submitCodeSchema } from '@/lib/validations';
import { SUPPORTED_LANGUAGES } from '@/types';

describe('Property 11: Code Submission Validation', () => {
  const validLanguageIds = SUPPORTED_LANGUAGES.map(l => l.id);

  it('should reject empty code', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...validLanguageIds),
        (languageId) => {
          const result = submitCodeSchema.safeParse({
            code: '',
            languageId,
          });

          expect(result.success).toBe(false);
          if (!result.success) {
            expect(result.error.issues.some(i => i.path.includes('code'))).toBe(true);
          }
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept valid code with valid languageId', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 10000 }),
        fc.constantFrom(...validLanguageIds),
        (code, languageId) => {
          const result = submitCodeSchema.safeParse({
            code,
            languageId,
          });

          expect(result.success).toBe(true);
          if (result.success) {
            expect(result.data.code).toBe(code);
            expect(result.data.languageId).toBe(languageId);
          }
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should reject invalid languageId (0 or negative)', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 500 }),
        fc.integer({ min: -1000, max: 0 }),
        (code, languageId) => {
          const result = submitCodeSchema.safeParse({
            code,
            languageId,
          });

          expect(result.success).toBe(false);
          if (!result.success) {
            expect(result.error.issues.some(i => i.path.includes('languageId'))).toBe(true);
          }
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept code with special characters', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 1000 }).map(s => 
          `public class Solution {\n  public static void main(String[] args) {\n    System.out.println("${s}");\n  }\n}`
        ),
        fc.constantFrom(...validLanguageIds),
        (code, languageId) => {
          const result = submitCodeSchema.safeParse({
            code,
            languageId,
          });

          expect(result.success).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept code with unicode characters', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 500, unit: 'grapheme' }),
        fc.constantFrom(...validLanguageIds),
        (code, languageId) => {
          const result = submitCodeSchema.safeParse({
            code,
            languageId,
          });

          expect(result.success).toBe(true);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should accept all supported language IDs', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 500 }),
        fc.constantFrom(...validLanguageIds),
        (code, languageId) => {
          const result = submitCodeSchema.safeParse({
            code,
            languageId,
          });

          expect(result.success).toBe(true);
          expect(validLanguageIds).toContain(languageId);
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should preserve code content exactly after validation', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 2000 }),
        fc.constantFrom(...validLanguageIds),
        (code, languageId) => {
          const result = submitCodeSchema.safeParse({
            code,
            languageId,
          });

          expect(result.success).toBe(true);
          if (result.success) {
            expect(result.data.code).toBe(code);
          }
        }
      ),
      { numRuns: 100 }
    );
  });
});
