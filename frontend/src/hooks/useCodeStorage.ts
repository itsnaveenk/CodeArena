import { useState, useEffect, useCallback } from 'react';

interface StoredCode {
  code: string;
  updatedAt: string;
}

function getStorageKey(problemId: number, languageId: number): string {
  return `codearena-code-${problemId}-${languageId}`;
}

export function useCodeStorage(problemId: number, languageId: number, initialCode: string) {
  const [code, setCodeState] = useState<string>(() => {
    if (typeof window === 'undefined') return initialCode;
    
    const key = getStorageKey(problemId, languageId);
    const stored = localStorage.getItem(key);
    if (stored) {
      try {
        const parsed: StoredCode = JSON.parse(stored);
        return parsed.code;
      } catch {
        return initialCode;
      }
    }
    return initialCode;
  });

  useEffect(() => {
    const key = getStorageKey(problemId, languageId);
    const stored = localStorage.getItem(key);
    if (stored) {
      try {
        const parsed: StoredCode = JSON.parse(stored);
        setCodeState(parsed.code);
      } catch {
        setCodeState(initialCode);
      }
    } else {
      setCodeState(initialCode);
    }
  }, [problemId, languageId, initialCode]);

  const setCode = useCallback((newCode: string) => {
    setCodeState(newCode);
    const key = getStorageKey(problemId, languageId);
    const data: StoredCode = {
      code: newCode,
      updatedAt: new Date().toISOString(),
    };
    localStorage.setItem(key, JSON.stringify(data));
  }, [problemId, languageId]);

  const clearCode = useCallback(() => {
    const key = getStorageKey(problemId, languageId);
    localStorage.removeItem(key);
    setCodeState(initialCode);
  }, [problemId, languageId, initialCode]);

  return { code, setCode, clearCode };
}
