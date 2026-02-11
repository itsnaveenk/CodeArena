import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import fc from 'fast-check';
import { AdminOverview } from './AdminOverview';
import type { PlatformStats } from '@/lib/api';

const mockUseAdminStats = vi.fn();
vi.mock('@/hooks/useAdminStats', () => ({
  useAdminStats: () => mockUseAdminStats(),
}));

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
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  );
};

const platformStatsArbitrary = (): fc.Arbitrary<PlatformStats> =>
  fc.record({
    totalUsers: fc.integer({ min: 0, max: 100000 }),
    usersByRole: fc.record({
      USER: fc.integer({ min: 0, max: 50000 }),
      PROBLEM_SETTER: fc.integer({ min: 0, max: 10000 }),
      ADMIN: fc.integer({ min: 0, max: 100 }),
    }),
    totalProblems: fc.integer({ min: 0, max: 10000 }),
    problemsByStatus: fc.record({
      DRAFT: fc.integer({ min: 0, max: 5000 }),
      PENDING_REVIEW: fc.integer({ min: 0, max: 1000 }),
      PUBLISHED: fc.integer({ min: 0, max: 5000 }),
      REJECTED: fc.integer({ min: 0, max: 500 }),
      ARCHIVED: fc.integer({ min: 0, max: 1000 }),
    }),
    problemsByDifficulty: fc.record({
      EASY: fc.integer({ min: 0, max: 3000 }),
      MEDIUM: fc.integer({ min: 0, max: 4000 }),
      HARD: fc.integer({ min: 0, max: 3000 }),
    }),
    totalSubmissions: fc.integer({ min: 0, max: 1000000 }),
    submissionsToday: fc.integer({ min: 0, max: 10000 }),
    acceptanceRate: fc.float({ min: 0, max: 1, noNaN: true }),
  });

describe('AdminOverview', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  describe('Unit Tests', () => {
    it('renders loading state correctly', () => {
      mockUseAdminStats.mockReturnValue({
        data: undefined,
        isLoading: true,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      const skeletons = document.querySelectorAll('[data-slot="skeleton"]');
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('renders error state correctly', () => {
      mockUseAdminStats.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: new Error('Failed to fetch'),
      });

      renderWithProviders(<AdminOverview />);

      expect(screen.getByText('Failed to load statistics')).toBeInTheDocument();
      expect(screen.getByText('Failed to fetch')).toBeInTheDocument();
    });

    it('renders stats when data is loaded', () => {
      const mockStats: PlatformStats = {
        totalUsers: 100,
        usersByRole: { USER: 80, PROBLEM_SETTER: 15, ADMIN: 5 },
        totalProblems: 50,
        problemsByStatus: {
          DRAFT: 10,
          PENDING_REVIEW: 5,
          PUBLISHED: 30,
          REJECTED: 3,
          ARCHIVED: 2,
        },
        problemsByDifficulty: { EASY: 15, MEDIUM: 20, HARD: 15 },
        totalSubmissions: 1000,
        submissionsToday: 50,
        acceptanceRate: 0.65,
      };

      mockUseAdminStats.mockReturnValue({
        data: mockStats,
        isLoading: false,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      expect(screen.getByText('Total Users')).toBeInTheDocument();
      expect(screen.getByText('Total Problems')).toBeInTheDocument();
      expect(screen.getByText('Total Submissions')).toBeInTheDocument();

      expect(screen.getByText('65.0%')).toBeInTheDocument();
    });
  });

  describe('Property Tests', () => {
    it('Property 22: displays all required statistics fields', () => {
      fc.assert(
        fc.property(platformStatsArbitrary(), (stats) => {
          mockUseAdminStats.mockReturnValue({
            data: stats,
            isLoading: false,
            error: null,
          });

          const { unmount } = renderWithProviders(<AdminOverview />);

          expect(screen.getByText('Total Users')).toBeInTheDocument();
          expect(screen.getByText('Total Problems')).toBeInTheDocument();
          expect(screen.getByText('Total Submissions')).toBeInTheDocument();
          expect(screen.getByText('Submissions Today')).toBeInTheDocument();

          expect(screen.getByText('Users by Role')).toBeInTheDocument();
          expect(screen.getByText('Problems by Status')).toBeInTheDocument();
          expect(screen.getByText('Problems by Difficulty')).toBeInTheDocument();

          expect(screen.getByText('Users')).toBeInTheDocument();
          expect(screen.getByText('Problem Setters')).toBeInTheDocument();
          expect(screen.getByText('Admins')).toBeInTheDocument();

          expect(screen.getByText('Published')).toBeInTheDocument();
          expect(screen.getByText('Pending Review')).toBeInTheDocument();
          expect(screen.getByText('Draft')).toBeInTheDocument();
          expect(screen.getByText('Rejected')).toBeInTheDocument();
          expect(screen.getByText('Archived')).toBeInTheDocument();

          expect(screen.getByText('Easy')).toBeInTheDocument();
          expect(screen.getByText('Medium')).toBeInTheDocument();
          expect(screen.getByText('Hard')).toBeInTheDocument();

          expect(screen.getByText('Platform Performance')).toBeInTheDocument();
          expect(screen.getByText('Overall Acceptance Rate')).toBeInTheDocument();

          unmount();
          return true;
        }),
        { numRuns: 30 }
      );
    }, 10000);

    it('Property 22b: displays correct numeric values for all statistics', () => {
      const stats: PlatformStats = {
        totalUsers: 12345,
        usersByRole: { USER: 11000, PROBLEM_SETTER: 1200, ADMIN: 145 },
        totalProblems: 567,
        problemsByStatus: {
          DRAFT: 111,
          PENDING_REVIEW: 222,
          PUBLISHED: 333,
          REJECTED: 44,
          ARCHIVED: 55,
        },
        problemsByDifficulty: { EASY: 166, MEDIUM: 277, HARD: 124 },
        totalSubmissions: 98765,
        submissionsToday: 432,
        acceptanceRate: 0.7531,
      };

      mockUseAdminStats.mockReturnValue({
        data: stats,
        isLoading: false,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      expect(screen.getByText('12345')).toBeInTheDocument();
      expect(screen.getByText('567')).toBeInTheDocument();
      expect(screen.getByText('98765')).toBeInTheDocument();
      expect(screen.getByText('432')).toBeInTheDocument();

      expect(screen.getByText('11000')).toBeInTheDocument();
      expect(screen.getByText('1200')).toBeInTheDocument();
      expect(screen.getByText('145')).toBeInTheDocument();

      expect(screen.getByText('333')).toBeInTheDocument();
      expect(screen.getByText('222')).toBeInTheDocument();
      expect(screen.getByText('111')).toBeInTheDocument();
      expect(screen.getByText('44')).toBeInTheDocument();
      expect(screen.getByText('55')).toBeInTheDocument();

      expect(screen.getByText('166')).toBeInTheDocument();
      expect(screen.getByText('277')).toBeInTheDocument();
      expect(screen.getByText('124')).toBeInTheDocument();

      expect(screen.getByText('75.3%')).toBeInTheDocument();
    });

    it('Property 22c: handles zero values correctly', () => {
      const zeroStats: PlatformStats = {
        totalUsers: 0,
        usersByRole: { USER: 0, PROBLEM_SETTER: 0, ADMIN: 0 },
        totalProblems: 0,
        problemsByStatus: {
          DRAFT: 0,
          PENDING_REVIEW: 0,
          PUBLISHED: 0,
          REJECTED: 0,
          ARCHIVED: 0,
        },
        problemsByDifficulty: { EASY: 0, MEDIUM: 0, HARD: 0 },
        totalSubmissions: 0,
        submissionsToday: 0,
        acceptanceRate: 0,
      };

      mockUseAdminStats.mockReturnValue({
        data: zeroStats,
        isLoading: false,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      const zeroElements = screen.getAllByText('0');
      expect(zeroElements.length).toBeGreaterThan(0);

      expect(screen.getByText('0.0%')).toBeInTheDocument();
    });

    it('Property 22d: handles large values correctly', () => {
      const largeStats: PlatformStats = {
        totalUsers: 999999,
        usersByRole: { USER: 900000, PROBLEM_SETTER: 99000, ADMIN: 999 },
        totalProblems: 50000,
        problemsByStatus: {
          DRAFT: 10000,
          PENDING_REVIEW: 5000,
          PUBLISHED: 30000,
          REJECTED: 3000,
          ARCHIVED: 2000,
        },
        problemsByDifficulty: { EASY: 15000, MEDIUM: 20000, HARD: 15000 },
        totalSubmissions: 9999999,
        submissionsToday: 99999,
        acceptanceRate: 0.9999,
      };

      mockUseAdminStats.mockReturnValue({
        data: largeStats,
        isLoading: false,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      expect(screen.getByText('999999')).toBeInTheDocument();
      expect(screen.getByText('9999999')).toBeInTheDocument();
      expect(screen.getByText('100.0%')).toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles undefined stats gracefully', () => {
      mockUseAdminStats.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      expect(screen.getByText('Total Users')).toBeInTheDocument();
    });

    it('handles partial stats data', () => {
      const partialStats = {
        totalUsers: 100,
        usersByRole: { USER: 80, PROBLEM_SETTER: 15, ADMIN: 5 },
        totalProblems: 50,
        problemsByStatus: {
          DRAFT: 10,
          PENDING_REVIEW: 5,
          PUBLISHED: 30,
          REJECTED: 3,
          ARCHIVED: 2,
        },
        problemsByDifficulty: { EASY: 15, MEDIUM: 20, HARD: 15 },
        totalSubmissions: 1000,
        submissionsToday: 50,
        acceptanceRate: 0.5,
      };

      mockUseAdminStats.mockReturnValue({
        data: partialStats,
        isLoading: false,
        error: null,
      });

      renderWithProviders(<AdminOverview />);

      expect(screen.getByText('100')).toBeInTheDocument();
      expect(screen.getByText('50.0%')).toBeInTheDocument();
    });
  });
});
