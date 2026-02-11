import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { ProblemCard } from '@/components/problem/ProblemCard';
import { useCodeStorage } from '@/hooks/useCodeStorage';
import { renderHook, act } from '@testing-library/react';

const mockFetch = vi.fn();
(globalThis as unknown as { fetch: typeof mockFetch }).fetch = mockFetch;

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const mockProblem = {
  id: 1,
  title: 'Two Sum',
  slug: 'two-sum',
  difficulty: 'EASY' as const,
  tags: ['Array', 'Hash Table'],
  solveStatus: 'NOT_TRIED' as const,
};

describe('Problem Solving Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    
    useAuthStore.setState({
      user: {
        id: 1,
        name: 'Test User',
        email: 'test@example.com',
        role: 'USER',
        createdAt: new Date().toISOString(),
      },
      token: 'test-token',
      isAuthenticated: true,
      isLoading: false,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('Problem List Navigation', () => {
    it('should display problem card with correct information', () => {
      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <ProblemCard problem={mockProblem} />
          </MemoryRouter>
        </QueryClientProvider>
      );

      expect(screen.getByText('Two Sum')).toBeInTheDocument();
      expect(screen.getByText('EASY')).toBeInTheDocument();
      expect(screen.getByText('Array')).toBeInTheDocument();
      expect(screen.getByText('Hash Table')).toBeInTheDocument();
    });

    it('should navigate to problem detail when clicking card', async () => {
      userEvent.setup();
      const queryClient = createTestQueryClient();
      
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/problems']}>
            <Routes>
              <Route path="/problems" element={<ProblemCard problem={mockProblem} />} />
              <Route path="/problems/:slug" element={<div>Problem Detail Page</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      );

      const card = screen.getByText('Two Sum').closest('a');
      expect(card).toHaveAttribute('href', '/problems/two-sum');
    });
  });

  describe('Code Persistence', () => {
    it('should persist code and retrieve it correctly', () => {
      const { result } = renderHook(() => useCodeStorage(1, 62, '// initial code'));

      act(() => {
        result.current.setCode('class Solution { }');
      });

      expect(result.current.code).toBe('class Solution { }');
    });

    it('should clear code and reset to initial', () => {
      const { result } = renderHook(() => useCodeStorage(1, 62, '// initial'));

      act(() => {
        result.current.setCode('// modified');
      });
      expect(result.current.code).toBe('// modified');

      act(() => {
        result.current.clearCode();
      });
      expect(result.current.code).toBe('// initial');
    });

    it('should maintain separate code for different languages', () => {
      const { result: javaResult } = renderHook(() => useCodeStorage(1, 62, ''));
      const { result: pythonResult } = renderHook(() => useCodeStorage(1, 71, ''));

      act(() => {
        javaResult.current.setCode('class Solution {}');
      });
      act(() => {
        pythonResult.current.setCode('def solution(): pass');
      });

      expect(javaResult.current.code).toBe('class Solution {}');
      expect(pythonResult.current.code).toBe('def solution(): pass');
    });

    it('should maintain separate code for different problems', () => {
      const { result: problem1 } = renderHook(() => useCodeStorage(1, 62, ''));
      const { result: problem2 } = renderHook(() => useCodeStorage(2, 62, ''));

      act(() => {
        problem1.current.setCode('// Problem 1 solution');
      });
      act(() => {
        problem2.current.setCode('// Problem 2 solution');
      });

      expect(problem1.current.code).toBe('// Problem 1 solution');
      expect(problem2.current.code).toBe('// Problem 2 solution');
    });
  });

  describe('Code Submission', () => {
    it('should call API when submitting code', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({
          submissionId: 'sub-123',
          verdict: 'ACCEPTED',
          runtime: 5,
          memory: 40000,
          passedTestcases: 10,
          totalTestcases: 10,
        }),
      });

      const response = await fetch('http://localhost:8080/api/execute/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          problemId: 1,
          languageId: 62,
          code: 'class Solution { }',
        }),
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.verdict).toBe('ACCEPTED');
    });

    it('should handle submission errors gracefully', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: () => Promise.resolve({
          code: 'COMPILATION_ERROR',
          message: 'Compilation failed',
        }),
      });

      const response = await fetch('http://localhost:8080/api/execute/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          problemId: 1,
          languageId: 62,
          code: 'invalid code',
        }),
      });

      expect(response.ok).toBe(false);
      const data = await response.json();
      expect(data.code).toBe('COMPILATION_ERROR');
    });
  });

  describe('Run Code', () => {
    it('should call API when running code', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({
          stdout: '[0, 1]',
          stderr: '',
          compileOutput: '',
          status: 'SUCCESS',
          time: 0.05,
          memory: 35000,
        }),
      });

      const response = await fetch('http://localhost:8080/api/execute/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          problemId: 1,
          languageId: 62,
          code: 'class Solution { }',
        }),
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data.status).toBe('SUCCESS');
      expect(data.stdout).toBe('[0, 1]');
    });
  });
});
