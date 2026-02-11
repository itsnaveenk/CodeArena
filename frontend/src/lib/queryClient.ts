import { QueryClient } from '@tanstack/react-query';
import type { ProblemFilter } from '@/types';
import type { ContestFilter } from '@/types/contest';
import type { MyProblemsFilter, UsersFilter, AllProblemsFilter, SubmissionsFilter } from './api';

export const queryKeys = {
  problems: {
    all: ['problems'] as const,
    list: (filter: ProblemFilter) => ['problems', 'list', filter] as const,
    detail: (id: number) => ['problems', 'detail', id] as const,
    bySlug: (slug: string) => ['problems', 'slug', slug] as const,
  },
  
  testcases: {
    all: ['testcases'] as const,
    byProblem: (problemId: number) => ['testcases', problemId] as const,
  },
  
  user: {
    all: ['user'] as const,
    current: () => ['user', 'current'] as const,
    stats: () => ['user', 'stats'] as const,
    submissions: (params?: { problemId?: number; page?: number }) => 
      ['user', 'submissions', params] as const,
  },
  
  myProblems: {
    all: ['myProblems'] as const,
    list: (filter?: MyProblemsFilter) => ['myProblems', 'list', filter] as const,
  },
  
  contests: {
    all: ['contests'] as const,
    list: (filter?: ContestFilter) => ['contests', 'list', filter] as const,
    detail: (id: number) => ['contests', 'detail', id] as const,
    bySlug: (slug: string) => ['contests', 'slug', slug] as const,
    problems: (contestId: number) => ['contests', contestId, 'problems'] as const,
    problem: (contestId: number, problemId: number) => ['contests', contestId, 'problems', problemId] as const,
    leaderboard: (contestId: number, page?: number) => ['contests', contestId, 'leaderboard', page] as const,
    myRanking: (contestId: number) => ['contests', contestId, 'myRanking'] as const,
    registration: (contestId: number) => ['contests', contestId, 'registration'] as const,
    submissions: (contestId: number, problemId?: number) => ['contests', contestId, 'submissions', problemId] as const,
    invitations: (contestId: number) => ['contests', contestId, 'invitations'] as const,
    statistics: (contestId: number) => ['contests', contestId, 'statistics'] as const,
    editorials: (contestId: number) => ['contests', contestId, 'editorials'] as const,
    myContests: () => ['contests', 'my'] as const,
    draft: (contestId: number, problemId: number) => ['contests', contestId, 'draft', problemId] as const,
  },
  
  admin: {
    all: ['admin'] as const,
    stats: () => ['admin', 'stats'] as const,
    pendingProblems: (page?: number) => ['admin', 'pending', page] as const,
    allSubmissions: (page?: number) => ['admin', 'submissions', page] as const,
    users: {
      all: ['admin', 'users'] as const,
      list: (filter?: UsersFilter) => ['admin', 'users', 'list', filter] as const,
    },
    problems: {
      all: ['admin', 'problems'] as const,
      list: (filter?: AllProblemsFilter) => ['admin', 'problems', 'list', filter] as const,
    },
    submissions: {
      all: ['admin', 'submissions'] as const,
      list: (filter?: SubmissionsFilter) => ['admin', 'submissions', 'list', filter] as const,
    },
  },
};

export const createQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000, // 30 seconds
      gcTime: 5 * 60 * 1000, // 5 minutes (formerly cacheTime)
      retry: (failureCount, error) => {
        if (error instanceof Error && 'code' in error) {
          const code = (error as { code: string }).code;
          if (['UNAUTHORIZED', 'FORBIDDEN', 'NOT_FOUND', 'VALIDATION_ERROR'].includes(code)) {
            return false;
          }
        }
        return failureCount < 3;
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});
