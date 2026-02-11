import { useQuery } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';

export function useAdminStats() {
  return useQuery({
    queryKey: queryKeys.admin.stats(),
    queryFn: () => getApiClient().getAdminStats(),
    staleTime: 60_000, // 1 minute
  });
}
