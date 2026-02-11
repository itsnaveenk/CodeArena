import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import type { RunRequest, SubmitRequest } from '@/types';

export function useRunCode() {
  return useMutation({
    mutationFn: (data: RunRequest) => getApiClient().runCode(data),
  });
}

export function useSubmitCode() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: SubmitRequest) => getApiClient().submitCode(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', 'stats'] });
      queryClient.invalidateQueries({ queryKey: ['user', 'submissions'] });
      queryClient.invalidateQueries({ queryKey: ['problems'] });
    },
  });
}
