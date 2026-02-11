import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient, type MyProblemsFilter } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { toast } from '@/lib/toast';

export function useMyProblems(filter?: MyProblemsFilter) {
  return useQuery({
    queryKey: queryKeys.myProblems.list(filter),
    queryFn: () => getApiClient().getMyProblems(filter),
  });
}

export function useDeleteProblem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => getApiClient().deleteProblem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.myProblems.all });
      toast.success('Problem deleted successfully');
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to delete problem');
    },
  });
}

export function useRequestReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => getApiClient().requestReview(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.myProblems.all });
      toast.success('Problem submitted for review');
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to submit for review');
    },
  });
}
