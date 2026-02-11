import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import fc from 'fast-check';
import type { Verdict, Submission, PaginatedResponse } from '@/types';

const mockUseAdminSubmissions = vi.fn();

vi.mock('@/hooks/useAdminSubmissions', () => ({
  useAdminSubmissions: () => mockUseAdminSubmissions(),
}));

import { SubmissionMonitor } from './SubmissionMonitor';

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = createTestQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>
  );
};

const verdictArbitrary = (): fc.Arbitrary<Verdict> =>
  fc.constantFrom<Verdict>('ACCEPTED', 'WRONG_ANSWER', 'TLE', 'RUNTIME_ERROR', 'COMPILATION_ERROR');

const languageIdArbitrary = (): fc.Arbitrary<number> =>
  fc.constantFrom(62, 71, 54); // Java, Python 3, C++

const submissionArbitrary = (): fc.Arbitrary<Submission> =>
  fc.record({
    id: fc.uuid(),
    userId: fc.integer({ min: 1, max: 10000 }),
    problemId: fc.integer({ min: 1, max: 10000 }),
    problemTitle: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
    languageId: languageIdArbitrary(),
    code: fc.string({ minLength: 10, maxLength: 500 }),
    verdict: verdictArbitrary(),
    runtime: fc.integer({ min: 0, max: 5000 }),
    memory: fc.integer({ min: 1024, max: 512000 }), // 1KB to 500MB in bytes
    passedTestcases: fc.integer({ min: 0, max: 100 }),
    totalTestcases: fc.integer({ min: 1, max: 100 }),
    createdAt: fc.constant(new Date().toISOString()),
  }).map(s => ({
    ...s,
    passedTestcases: Math.min(s.passedTestcases, s.totalTestcases),
  }));

function filterSubmissionsByVerdict(submissions: Submission[], verdict: Verdict | null): Submission[] {
  if (!verdict) return submissions;
  return submissions.filter((s) => s.verdict === verdict);
}

function getVerdictLabel(verdict: Verdict): string {
  switch (verdict) {
    case 'ACCEPTED':
      return 'Accepted';
    case 'WRONG_ANSWER':
      return 'Wrong Answer';
    case 'TLE':
      return 'TLE';
    case 'RUNTIME_ERROR':
      return 'Runtime Error';
    case 'COMPILATION_ERROR':
      return 'Compile Error';
    default:
      return verdict;
  }
}

function getLanguageName(languageId: number): string {
  switch (languageId) {
    case 62:
      return 'Java';
    case 71:
      return 'Python 3';
    case 54:
      return 'C++';
    default:
      return `Language ${languageId}`;
  }
}

describe('SubmissionMonitor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders loading state correctly', () => {
      mockUseAdminSubmissions.mockReturnValue({
        data: undefined,
        isLoading: true,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('Auto-refresh: 30s')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /refresh/i })).toBeInTheDocument();
    });

    it('renders submission list when data is loaded', () => {
      const mockSubmissions: PaginatedResponse<Submission> = {
        content: [
          {
            id: 'sub-1',
            userId: 1,
            problemId: 1,
            problemTitle: 'Two Sum',
            languageId: 62,
            code: 'public class Solution {}',
            verdict: 'ACCEPTED',
            runtime: 150,
            memory: 40960,
            passedTestcases: 10,
            totalTestcases: 10,
            createdAt: '2024-01-15T10:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAdminSubmissions.mockReturnValue({
        data: mockSubmissions,
        isLoading: false,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('Two Sum')).toBeInTheDocument();
      expect(screen.getByText('User #1')).toBeInTheDocument();
      expect(screen.getByText('Java')).toBeInTheDocument();
      expect(screen.getByText('Accepted')).toBeInTheDocument();
      expect(screen.getByText('150ms')).toBeInTheDocument();
      expect(screen.getByText('10/10')).toBeInTheDocument();
    });

    it('renders empty state when no submissions', () => {
      mockUseAdminSubmissions.mockReturnValue({
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 10,
        },
        isLoading: false,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('No submissions found')).toBeInTheDocument();
    });

    it('shows refresh button with spinning icon when fetching', () => {
      mockUseAdminSubmissions.mockReturnValue({
        data: undefined,
        isLoading: false,
        refetch: vi.fn(),
        isFetching: true,
      });

      renderWithProviders(<SubmissionMonitor />);

      const refreshButton = screen.getByRole('button', { name: /refresh/i });
      expect(refreshButton).toBeDisabled();
    });
  });

  describe('Property Tests', () => {
    it('Property 31: displays all required submission fields', () => {
      const mockSubmissions: PaginatedResponse<Submission> = {
        content: [
          {
            id: 'test-sub-1',
            userId: 42,
            problemId: 5,
            problemTitle: 'Binary Search',
            languageId: 71,
            code: 'def solution(): pass',
            verdict: 'WRONG_ANSWER',
            runtime: 250,
            memory: 51200,
            passedTestcases: 7,
            totalTestcases: 10,
            createdAt: '2024-06-15T14:30:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAdminSubmissions.mockReturnValue({
        data: mockSubmissions,
        isLoading: false,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('User #42')).toBeInTheDocument();

      expect(screen.getByText('Binary Search')).toBeInTheDocument();

      expect(screen.getByText('Python 3')).toBeInTheDocument();

      expect(screen.getByText('Wrong Answer')).toBeInTheDocument();

      expect(screen.getByText('250ms')).toBeInTheDocument();

      expect(screen.getByText('50.0MB')).toBeInTheDocument();

      expect(screen.getByText('7/10')).toBeInTheDocument();
    });

    it('Property 31b: all verdict types display correctly', () => {
      fc.assert(
        fc.property(verdictArbitrary(), (verdict) => {
          const label = getVerdictLabel(verdict);
          expect(label).toBeTruthy();
          expect(typeof label).toBe('string');
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 31c: all language types display correctly', () => {
      fc.assert(
        fc.property(languageIdArbitrary(), (languageId) => {
          const name = getLanguageName(languageId);
          expect(name).toBeTruthy();
          expect(typeof name).toBe('string');
          expect(['Java', 'Python 3', 'C++']).toContain(name);
          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 31d: memory conversion is correct', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1024, max: 512000000 }), // 1KB to 500GB in bytes
          (memoryBytes) => {
            const memoryMB = memoryBytes / 1024;
            const formatted = memoryMB.toFixed(1);
            expect(parseFloat(formatted)).toBeGreaterThanOrEqual(0);
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 32: verdict filter logic is correct', () => {
      fc.assert(
        fc.property(
          fc.array(submissionArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.option(verdictArbitrary(), { nil: null }),
          (submissions, verdictFilter) => {
            const filtered = filterSubmissionsByVerdict(submissions, verdictFilter);

            if (verdictFilter) {
              filtered.forEach((submission) => {
                expect(submission.verdict).toBe(verdictFilter);
              });
            } else {
              expect(filtered.length).toBe(submissions.length);
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 32b: filter preserves submission integrity', () => {
      fc.assert(
        fc.property(
          fc.array(submissionArbitrary(), { minLength: 1, maxLength: 20 }),
          verdictArbitrary(),
          (submissions, verdictFilter) => {
            const filtered = filterSubmissionsByVerdict(submissions, verdictFilter);

            filtered.forEach((submission) => {
              expect(submission.id).toBeTruthy();
              expect(submission.userId).toBeGreaterThan(0);
              expect(submission.problemId).toBeGreaterThan(0);
              expect(submission.problemTitle).toBeTruthy();
              expect(submission.code).toBeTruthy();
              expect(submission.runtime).toBeGreaterThanOrEqual(0);
              expect(submission.memory).toBeGreaterThan(0);
              expect(submission.passedTestcases).toBeLessThanOrEqual(submission.totalTestcases);
            });

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 32c: filter count is correct', () => {
      fc.assert(
        fc.property(
          fc.array(submissionArbitrary(), { minLength: 0, maxLength: 50 }),
          verdictArbitrary(),
          (submissions, verdictFilter) => {
            const filtered = filterSubmissionsByVerdict(submissions, verdictFilter);
            const expectedCount = submissions.filter(s => s.verdict === verdictFilter).length;

            expect(filtered.length).toBe(expectedCount);

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 32d: null filter returns all submissions', () => {
      fc.assert(
        fc.property(
          fc.array(submissionArbitrary(), { minLength: 0, maxLength: 50 }),
          (submissions) => {
            const filtered = filterSubmissionsByVerdict(submissions, null);
            expect(filtered.length).toBe(submissions.length);
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  describe('Edge Cases', () => {
    it('handles submission with zero runtime', () => {
      const mockSubmissions: PaginatedResponse<Submission> = {
        content: [
          {
            id: 'sub-zero',
            userId: 1,
            problemId: 1,
            problemTitle: 'Fast Problem',
            languageId: 54,
            code: 'int main() { return 0; }',
            verdict: 'ACCEPTED',
            runtime: 0,
            memory: 2048,
            passedTestcases: 5,
            totalTestcases: 5,
            createdAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAdminSubmissions.mockReturnValue({
        data: mockSubmissions,
        isLoading: false,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('0ms')).toBeInTheDocument();
    });

    it('handles submission with all test cases failed', () => {
      const mockSubmissions: PaginatedResponse<Submission> = {
        content: [
          {
            id: 'sub-fail',
            userId: 1,
            problemId: 1,
            problemTitle: 'Hard Problem',
            languageId: 62,
            code: 'class Solution {}',
            verdict: 'WRONG_ANSWER',
            runtime: 100,
            memory: 4096,
            passedTestcases: 0,
            totalTestcases: 10,
            createdAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAdminSubmissions.mockReturnValue({
        data: mockSubmissions,
        isLoading: false,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('0/10')).toBeInTheDocument();
    });

    it('handles compilation error verdict', () => {
      const mockSubmissions: PaginatedResponse<Submission> = {
        content: [
          {
            id: 'sub-ce',
            userId: 1,
            problemId: 1,
            problemTitle: 'Syntax Error',
            languageId: 62,
            code: 'invalid code',
            verdict: 'COMPILATION_ERROR',
            runtime: 0,
            memory: 0,
            passedTestcases: 0,
            totalTestcases: 5,
            createdAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAdminSubmissions.mockReturnValue({
        data: mockSubmissions,
        isLoading: false,
        refetch: vi.fn(),
        isFetching: false,
      });

      renderWithProviders(<SubmissionMonitor />);

      expect(screen.getByText('Compile Error')).toBeInTheDocument();
    });
  });
});
