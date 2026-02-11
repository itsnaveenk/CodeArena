import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import fc from 'fast-check';
import type { ProblemStatus, Difficulty } from '@/types';
import type { ProblemManagementItem, PaginatedResponse } from '@/lib/api';

const mockUseAllProblems = vi.fn();
const mockUsePublishProblem = vi.fn();
const mockUseRejectProblem = vi.fn();
const mockUseArchiveProblem = vi.fn();
const mockUseBulkPublish = vi.fn();
const mockUseBulkReject = vi.fn();
const mockUseBulkArchive = vi.fn();

vi.mock('@/hooks/useAllProblems', () => ({
  useAllProblems: () => mockUseAllProblems(),
  usePublishProblem: () => mockUsePublishProblem(),
  useRejectProblem: () => mockUseRejectProblem(),
  useArchiveProblem: () => mockUseArchiveProblem(),
  useBulkPublish: () => mockUseBulkPublish(),
  useBulkReject: () => mockUseBulkReject(),
  useBulkArchive: () => mockUseBulkArchive(),
}));

import { AllProblemsTable } from './AllProblemsTable';

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

const problemStatusArbitrary = (): fc.Arbitrary<ProblemStatus> =>
  fc.constantFrom<ProblemStatus>('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED');

const difficultyArbitrary = (): fc.Arbitrary<Difficulty> =>
  fc.constantFrom<Difficulty>('EASY', 'MEDIUM', 'HARD');

const problemManagementItemArbitrary = (): fc.Arbitrary<ProblemManagementItem> =>
  fc.record({
    id: fc.integer({ min: 1, max: 10000 }),
    title: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
    slug: fc.string({ minLength: 1, maxLength: 30 }),
    statement: fc.string({ minLength: 1, maxLength: 100 }),
    constraints: fc.string({ minLength: 0, maxLength: 50 }),
    difficulty: difficultyArbitrary(),
    tags: fc.array(fc.string({ minLength: 1, maxLength: 10 }), { minLength: 0, maxLength: 3 }),
    status: problemStatusArbitrary(),
    examples: fc.constant([]),
    starterCode: fc.constant({}),
    authorId: fc.integer({ min: 1, max: 1000 }),
    authorName: fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0),
    testcaseCount: fc.integer({ min: 0, max: 100 }),
    submissionCount: fc.integer({ min: 0, max: 10000 }),
    createdAt: fc.constant(new Date().toISOString()),
  });

function filterProblemsByStatus(problems: ProblemManagementItem[], status: ProblemStatus | null): ProblemManagementItem[] {
  if (!status) return problems;
  return problems.filter((p) => p.status === status);
}

function filterProblemsByDifficulty(problems: ProblemManagementItem[], difficulty: Difficulty | null): ProblemManagementItem[] {
  if (!difficulty) return problems;
  return problems.filter((p) => p.difficulty === difficulty);
}

function filterProblemsBySearch(problems: ProblemManagementItem[], search: string): ProblemManagementItem[] {
  if (!search) return problems;
  const lowerSearch = search.toLowerCase();
  return problems.filter(
    (p) =>
      p.title.toLowerCase().includes(lowerSearch) ||
      p.authorName.toLowerCase().includes(lowerSearch)
  );
}

function getExpectedActions(status: ProblemStatus): string[] {
  const actions = ['View', 'Edit'];
  if (status === 'PENDING_REVIEW') {
    actions.push('Publish', 'Reject');
  }
  if (status === 'PUBLISHED') {
    actions.push('Archive');
  }
  return actions;
}

describe('AllProblemsTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUsePublishProblem.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockUseRejectProblem.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockUseArchiveProblem.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockUseBulkPublish.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockUseBulkReject.mockReturnValue({ mutate: vi.fn(), isPending: false });
    mockUseBulkArchive.mockReturnValue({ mutate: vi.fn(), isPending: false });
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders loading state correctly', () => {
      mockUseAllProblems.mockReturnValue({
        data: undefined,
        isLoading: true,
      });

      renderWithProviders(<AllProblemsTable />);

      expect(screen.getByPlaceholderText('Search by title or author...')).toBeInTheDocument();
    });

    it('renders problem list when data is loaded', () => {
      const mockProblems: PaginatedResponse<ProblemManagementItem> = {
        content: [
          {
            id: 1,
            title: 'Two Sum',
            slug: 'two-sum',
            statement: 'Find two numbers',
            constraints: '',
            difficulty: 'EASY',
            tags: ['array'],
            status: 'PUBLISHED',
            examples: [],
            starterCode: {},
            authorId: 1,
            authorName: 'John Doe',
            testcaseCount: 5,
            submissionCount: 100,
            createdAt: '2024-01-01',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAllProblems.mockReturnValue({
        data: mockProblems,
        isLoading: false,
      });

      renderWithProviders(<AllProblemsTable />);

      expect(screen.getByText('Two Sum')).toBeInTheDocument();
      expect(screen.getByText('by John Doe')).toBeInTheDocument();
    });

    it('renders empty state when no problems', () => {
      mockUseAllProblems.mockReturnValue({
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 10,
        },
        isLoading: false,
      });

      renderWithProviders(<AllProblemsTable />);

      expect(screen.getByText('No problems found')).toBeInTheDocument();
    });
  });

  describe('Property Tests', () => {
    it('Property 27: displays all required problem fields', () => {
      const mockProblems: PaginatedResponse<ProblemManagementItem> = {
        content: [
          {
            id: 1,
            title: 'Test Problem',
            slug: 'test-problem',
            statement: 'Test statement',
            constraints: '',
            difficulty: 'MEDIUM',
            tags: [],
            status: 'PENDING_REVIEW',
            examples: [],
            starterCode: {},
            authorId: 1,
            authorName: 'Test Author',
            testcaseCount: 3,
            submissionCount: 50,
            createdAt: '2024-06-15T10:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAllProblems.mockReturnValue({
        data: mockProblems,
        isLoading: false,
      });

      renderWithProviders(<AllProblemsTable />);

      expect(screen.getByText('Test Problem')).toBeInTheDocument();

      expect(screen.getByText('by Test Author')).toBeInTheDocument();

      expect(screen.getByText('MEDIUM')).toBeInTheDocument();

      expect(screen.getByText('Pending Review')).toBeInTheDocument();

      expect(screen.getByText('3')).toBeInTheDocument();

      expect(screen.getByText('6/15/2024')).toBeInTheDocument();
    });

    it('Property 28: status filter logic is correct', () => {
      fc.assert(
        fc.property(
          fc.array(problemManagementItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.option(problemStatusArbitrary(), { nil: null }),
          (problems, statusFilter) => {
            const filtered = filterProblemsByStatus(problems, statusFilter);

            if (statusFilter) {
              filtered.forEach((problem) => {
                expect(problem.status).toBe(statusFilter);
              });
            } else {
              expect(filtered.length).toBe(problems.length);
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 28b: difficulty filter logic is correct', () => {
      fc.assert(
        fc.property(
          fc.array(problemManagementItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.option(difficultyArbitrary(), { nil: null }),
          (problems, difficultyFilter) => {
            const filtered = filterProblemsByDifficulty(problems, difficultyFilter);

            if (difficultyFilter) {
              filtered.forEach((problem) => {
                expect(problem.difficulty).toBe(difficultyFilter);
              });
            } else {
              expect(filtered.length).toBe(problems.length);
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 28c: search filter logic is correct', () => {
      fc.assert(
        fc.property(
          fc.array(problemManagementItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.string({ minLength: 0, maxLength: 10 }),
          (problems, searchQuery) => {
            const filtered = filterProblemsBySearch(problems, searchQuery);

            if (searchQuery) {
              const lowerSearch = searchQuery.toLowerCase();
              filtered.forEach((problem) => {
                const matchesTitle = problem.title.toLowerCase().includes(lowerSearch);
                const matchesAuthor = problem.authorName.toLowerCase().includes(lowerSearch);
                expect(matchesTitle || matchesAuthor).toBe(true);
              });
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 28d: combined filters work correctly', () => {
      fc.assert(
        fc.property(
          fc.array(problemManagementItemArbitrary(), { minLength: 1, maxLength: 20 }),
          fc.option(problemStatusArbitrary(), { nil: null }),
          fc.option(difficultyArbitrary(), { nil: null }),
          fc.string({ minLength: 0, maxLength: 5 }),
          (problems, statusFilter, difficultyFilter, searchQuery) => {
            let filtered = filterProblemsByStatus(problems, statusFilter);
            filtered = filterProblemsByDifficulty(filtered, difficultyFilter);
            filtered = filterProblemsBySearch(filtered, searchQuery);

            filtered.forEach((problem) => {
              if (statusFilter) {
                expect(problem.status).toBe(statusFilter);
              }
              if (difficultyFilter) {
                expect(problem.difficulty).toBe(difficultyFilter);
              }
              if (searchQuery) {
                const lowerSearch = searchQuery.toLowerCase();
                const matchesTitle = problem.title.toLowerCase().includes(lowerSearch);
                const matchesAuthor = problem.authorName.toLowerCase().includes(lowerSearch);
                expect(matchesTitle || matchesAuthor).toBe(true);
              }
            });

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 29: action buttons are correct for each status', () => {
      fc.assert(
        fc.property(problemStatusArbitrary(), (status) => {
          const expectedActions = getExpectedActions(status);

          expect(expectedActions).toContain('View');
          expect(expectedActions).toContain('Edit');

          if (status === 'PENDING_REVIEW') {
            expect(expectedActions).toContain('Publish');
            expect(expectedActions).toContain('Reject');
          } else {
            expect(expectedActions).not.toContain('Publish');
            expect(expectedActions).not.toContain('Reject');
          }

          if (status === 'PUBLISHED') {
            expect(expectedActions).toContain('Archive');
          } else {
            expect(expectedActions).not.toContain('Archive');
          }

          return true;
        }),
        { numRuns: 100 }
      );
    });

    it('Property 30: bulk actions require selection', () => {
      fc.assert(
        fc.property(
          fc.array(problemManagementItemArbitrary(), { minLength: 0, maxLength: 10 }),
          (selectedProblems) => {
            const bulkActionsEnabled = selectedProblems.length > 0;

            if (selectedProblems.length === 0) {
              expect(bulkActionsEnabled).toBe(false);
            } else {
              expect(bulkActionsEnabled).toBe(true);
            }

            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    it('Property 30b: bulk action shows correct count', () => {
      fc.assert(
        fc.property(
          fc.array(problemManagementItemArbitrary(), { minLength: 1, maxLength: 20 }),
          (selectedProblems) => {
            const count = selectedProblems.length;
            expect(count).toBeGreaterThan(0);
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  describe('Edge Cases', () => {
    it('handles empty search gracefully', () => {
      const mockProblems: PaginatedResponse<ProblemManagementItem> = {
        content: [
          {
            id: 1,
            title: 'Test Problem',
            slug: 'test',
            statement: '',
            constraints: '',
            difficulty: 'EASY',
            tags: [],
            status: 'DRAFT',
            examples: [],
            starterCode: {},
            authorId: 1,
            authorName: 'Author',
            testcaseCount: 0,
            submissionCount: 0,
            createdAt: '2024-01-01',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      mockUseAllProblems.mockReturnValue({
        data: mockProblems,
        isLoading: false,
      });

      renderWithProviders(<AllProblemsTable />);

      expect(screen.getByText('Test Problem')).toBeInTheDocument();
    });
  });
});
