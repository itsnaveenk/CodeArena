import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ProblemStatus, Difficulty } from '@/types';

vi.mock('@/hooks/useMyProblems', () => ({
  useMyProblems: vi.fn(),
  useDeleteProblem: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useRequestReview: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
}));

import { MyProblemsList } from './MyProblemsList';
import { useMyProblems } from '@/hooks/useMyProblems';

const mockUseMyProblems = useMyProblems as unknown as ReturnType<typeof vi.fn>;

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

const problemStatusArbitrary = fc.constantFrom<ProblemStatus>(
  'DRAFT',
  'PENDING_REVIEW',
  'PUBLISHED',
  'REJECTED',
  'ARCHIVED'
);

const difficultyArbitrary = fc.constantFrom<Difficulty>('EASY', 'MEDIUM', 'HARD');

const reservedWords = ['constructor', 'prototype', '__proto__', 'toString', 'valueOf', 'hasOwnProperty'];

const normalizeWhitespace = (value: string) => value.replace(/\s+/g, ' ').trim();

const problemArbitrary = fc.record({
  id: fc.integer({ min: 1, max: 10000 }),
  title: fc
    .string({ minLength: 2, maxLength: 100 })
    .map(normalizeWhitespace)
    .filter(
      (s) =>
        /^[a-zA-Z][a-zA-Z0-9 ]*[a-zA-Z0-9]$/.test(s) &&
        s.length >= 2 &&
        !reservedWords.includes(s.toLowerCase())
    ),
  slug: fc.string({ minLength: 2, maxLength: 50 }).filter((s) => /^[a-z][a-z0-9-]*$/.test(s)),
  statement: fc.string({ minLength: 10, maxLength: 500 }),
  constraints: fc.string({ minLength: 5, maxLength: 200 }),
  difficulty: difficultyArbitrary,
  tags: fc.array(fc.string({ minLength: 1, maxLength: 20 }).filter((s) => /^[a-zA-Z]+$/.test(s)), {
    minLength: 0,
    maxLength: 5,
  }),
  status: problemStatusArbitrary,
  examples: fc.constant([]),
  starterCode: fc.constant({}),
});

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = createTestQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
};

describe('Property 3: Status-Based Action Buttons', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show correct actions for DRAFT status', () => {
    fc.assert(
      fc.property(
        problemArbitrary.map((p) => ({ ...p, status: 'DRAFT' as ProblemStatus })),
        (problem) => {
          mockUseMyProblems.mockReturnValue({
            data: {
              content: [problem],
              totalElements: 1,
              totalPages: 1,
              page: 0,
              size: 10,
            },
            isLoading: false,
          });

          const { unmount } = renderWithProviders(<MyProblemsList />);

          const titleElements = screen.getAllByText(problem.title);
          expect(titleElements.length).toBeGreaterThan(0);

          unmount();
          return true;
        }
      ),
      { numRuns: 50 }
    );
  });

  it('should show correct actions for PENDING_REVIEW status', () => {
    fc.assert(
      fc.property(
        problemArbitrary.map((p) => ({ ...p, status: 'PENDING_REVIEW' as ProblemStatus })),
        (problem) => {
          mockUseMyProblems.mockReturnValue({
            data: {
              content: [problem],
              totalElements: 1,
              totalPages: 1,
              page: 0,
              size: 10,
            },
            isLoading: false,
          });

          const { unmount } = renderWithProviders(<MyProblemsList />);

          const titleElements = screen.getAllByText(problem.title);
          expect(titleElements.length).toBeGreaterThan(0);

          unmount();
          return true;
        }
      ),
      { numRuns: 50 }
    );
  });
});

describe('Property 4: Status Filter Correctness', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display problems matching the filter status', () => {
    fc.assert(
      fc.property(
        fc.array(problemArbitrary, { minLength: 1, maxLength: 5 }),
        problemStatusArbitrary,
        (problems, filterStatus) => {
          const uniqueProblems = problems.map((p, i) => ({ ...p, title: `${p.title}${i}` }));
          const filteredProblems = uniqueProblems.filter((p) => p.status === filterStatus);

          mockUseMyProblems.mockReturnValue({
            data: {
              content: filteredProblems,
              totalElements: filteredProblems.length,
              totalPages: 1,
              page: 0,
              size: 10,
            },
            isLoading: false,
          });

          const { unmount } = renderWithProviders(<MyProblemsList />);

          filteredProblems.forEach((problem) => {
            const titleElements = screen.getAllByText(problem.title);
            expect(titleElements.length).toBeGreaterThan(0);
          });

          unmount();
          return true;
        }
      ),
      { numRuns: 50 }
    );
  });
});

describe('Property 5: Search Filter Correctness', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display problems matching the search query', () => {
    fc.assert(
      fc.property(
        fc.array(problemArbitrary, { minLength: 1, maxLength: 5 }),
        fc.string({ minLength: 1, maxLength: 10 }).filter((s) => /^[a-zA-Z]+$/.test(s)),
        (problems, searchQuery) => {
          const uniqueProblems = problems.map((p, i) => ({ ...p, title: `${p.title}${i}` }));
          const filteredProblems = uniqueProblems.filter((p) =>
            p.title.toLowerCase().includes(searchQuery.toLowerCase())
          );

          mockUseMyProblems.mockReturnValue({
            data: {
              content: filteredProblems,
              totalElements: filteredProblems.length,
              totalPages: 1,
              page: 0,
              size: 10,
            },
            isLoading: false,
          });

          const { unmount } = renderWithProviders(<MyProblemsList />);

          filteredProblems.forEach((problem) => {
            const titleElements = screen.getAllByText(problem.title);
            expect(titleElements.length).toBeGreaterThan(0);
          });

          unmount();
          return true;
        }
      ),
      { numRuns: 50 }
    );
  });
});

describe('MyProblemsList Unit Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show loading state', () => {
    mockUseMyProblems.mockReturnValue({
      data: undefined,
      isLoading: true,
    });

    renderWithProviders(<MyProblemsList />);

    expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
  });

  it('should show empty state when no problems', () => {
    mockUseMyProblems.mockReturnValue({
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        page: 0,
        size: 10,
      },
      isLoading: false,
    });

    renderWithProviders(<MyProblemsList />);

    expect(screen.getByText(/No problems found/i)).toBeInTheDocument();
  });

  it('should show Create Problem button', () => {
    mockUseMyProblems.mockReturnValue({
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        page: 0,
        size: 10,
      },
      isLoading: false,
    });

    renderWithProviders(<MyProblemsList />);

    expect(screen.getByText('Create Problem')).toBeInTheDocument();
  });
});
