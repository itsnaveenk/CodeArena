import { useQuery } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import type { ProblemFilter } from '@/types';

export function useProblems(filter: ProblemFilter) {
  return useQuery({
    queryKey: queryKeys.problems.list(filter),
    queryFn: () => getApiClient().getProblems(filter),
    staleTime: 30_000,
  });
}

export function useProblem(id: number) {
  return useQuery({
    queryKey: queryKeys.problems.detail(id),
    queryFn: () => getApiClient().getProblem(id),
    staleTime: 60_000,
    enabled: id > 0,
  });
}

export function useProblemBySlug(slug: string) {
  return useQuery({
    queryKey: queryKeys.problems.bySlug(slug),
    queryFn: () => getApiClient().getProblemBySlug(slug),
    staleTime: 60_000,
    enabled: !!slug,
  });
}
