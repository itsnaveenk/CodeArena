import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient, type AllProblemsFilter } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { toast } from '@/lib/toast';

export function useAllProblems(filter?: AllProblemsFilter) {
  return useQuery({
    queryKey: queryKeys.admin.problems.list(filter),
    queryFn: () => getApiClient().getAllProblemsAdmin(filter),
  });
}

export function usePublishProblem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => getApiClient().publishProblem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.problems.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.problems.all });
      toast.success('Problem published successfully');
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to publish problem');
    },
  });
}

export function useRejectProblem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => getApiClient().rejectProblem(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.problems.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      toast.success('Problem rejected');
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to reject problem');
    },
  });
}

export function useArchiveProblem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => getApiClient().archiveProblem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.problems.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.problems.all });
      toast.success('Problem archived');
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to archive problem');
    },
  });
}

export function useBulkPublish() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ids: number[]) => getApiClient().bulkPublishProblems(ids),
    onSuccess: (_, ids) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.problems.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.problems.all });
      toast.success(`${ids.length} problems published successfully`);
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to publish problems');
    },
  });
}

export function useBulkReject() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ids: number[]) => getApiClient().bulkRejectProblems(ids),
    onSuccess: (_, ids) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.problems.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      toast.success(`${ids.length} problems rejected`);
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to reject problems');
    },
  });
}

export function useBulkArchive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ids: number[]) => getApiClient().bulkArchiveProblems(ids),
    onSuccess: (_, ids) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.problems.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.stats() });
      queryClient.invalidateQueries({ queryKey: queryKeys.problems.all });
      toast.success(`${ids.length} problems archived`);
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to archive problems');
    },
  });
}
