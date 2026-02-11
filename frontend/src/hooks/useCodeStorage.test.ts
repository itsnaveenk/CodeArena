import { describe, it, expect, beforeEach } from 'vitest';
import * as fc from 'fast-check';
import { renderHook, act } from '@testing-library/react';
import { useCodeStorage } from './useCodeStorage';

describe('Property 10: Code Persistence Round-Trip', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should persist code and retrieve it correctly', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 10000 }),
        fc.constantFrom(62, 71, 54), // Java, Python, C++
        fc.string({ minLength: 1, maxLength: 200 }).filter(s => /^[a-zA-Z0-9 ]+$/.test(s)),
        (problemId, languageId, newCode) => {
          localStorage.clear();
          
          const { result, unmount } = renderHook(() =>
            useCodeStorage(problemId, languageId, '')
          );

          act(() => {
            result.current.setCode(newCode);
          });

          expect(result.current.code).toBe(newCode);

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should clear code and reset to initial when clearCode is called', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 10000 }),
        fc.constantFrom(62, 71, 54),
        fc.string({ minLength: 1, maxLength: 100 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        fc.string({ minLength: 1, maxLength: 100 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        (problemId, languageId, initialCode, savedCode) => {
          localStorage.clear();
          
          const { result, unmount } = renderHook(() =>
            useCodeStorage(problemId, languageId, initialCode)
          );

          act(() => {
            result.current.setCode(savedCode);
          });
          expect(result.current.code).toBe(savedCode);

          act(() => {
            result.current.clearCode();
          });

          expect(result.current.code).toBe(initialCode);

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should return initial code when no persisted code exists', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 10000 }),
        fc.constantFrom(62, 71, 54),
        fc.string({ minLength: 1, maxLength: 100 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        (problemId, languageId, initialCode) => {
          localStorage.clear();

          const { result, unmount } = renderHook(() =>
            useCodeStorage(problemId, languageId, initialCode)
          );

          expect(result.current.code).toBe(initialCode);

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should handle different languages for same problem independently', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 10000 }),
        fc.string({ minLength: 1, maxLength: 50 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        fc.string({ minLength: 1, maxLength: 50 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        (problemId, javaCode, pythonCode) => {
          localStorage.clear();
          
          const { result: javaResult, unmount: unmountJava } = renderHook(() =>
            useCodeStorage(problemId, 62, '') // Java
          );
          const { result: pythonResult, unmount: unmountPython } = renderHook(() =>
            useCodeStorage(problemId, 71, '') // Python
          );

          act(() => {
            javaResult.current.setCode(javaCode);
          });
          act(() => {
            pythonResult.current.setCode(pythonCode);
          });

          expect(javaResult.current.code).toBe(javaCode);
          expect(pythonResult.current.code).toBe(pythonCode);

          unmountJava();
          unmountPython();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should use unique storage key per problemId', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 1000 }),
        fc.integer({ min: 1001, max: 2000 }),
        fc.constantFrom(62, 71, 54),
        fc.string({ minLength: 1, maxLength: 50 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        fc.string({ minLength: 1, maxLength: 50 }).filter(s => /^[a-zA-Z0-9]+$/.test(s)),
        (problemId1, problemId2, languageId, code1, code2) => {
          localStorage.clear();
          
          const { result: result1, unmount: unmount1 } = renderHook(() =>
            useCodeStorage(problemId1, languageId, '')
          );
          const { result: result2, unmount: unmount2 } = renderHook(() =>
            useCodeStorage(problemId2, languageId, '')
          );

          act(() => {
            result1.current.setCode(code1);
          });
          act(() => {
            result2.current.setCode(code2);
          });

          expect(result1.current.code).toBe(code1);
          expect(result2.current.code).toBe(code2);

          unmount1();
          unmount2();
        }
      ),
      { numRuns: 100 }
    );
  });

  it('should preserve code content exactly after set', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 10000 }),
        fc.constantFrom(62, 71, 54),
        fc.string({ minLength: 1, maxLength: 500 }).filter(s => /^[a-zA-Z0-9\s\n\t{}();=+\-*/<>!]+$/.test(s)),
        (problemId, languageId, code) => {
          localStorage.clear();
          
          const { result, unmount } = renderHook(() =>
            useCodeStorage(problemId, languageId, '')
          );

          act(() => {
            result.current.setCode(code);
          });

          expect(result.current.code).toBe(code);

          unmount();
        }
      ),
      { numRuns: 100 }
    );
  });
});
