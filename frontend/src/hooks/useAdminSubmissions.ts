import { useQuery } from '@tanstack/react-query';
import { getApiClient, type SubmissionsFilter } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';

interface UseAdminSubmissionsOptions extends SubmissionsFilter {
  autoRefresh?: boolean;
  refreshInterval?: number;
}

export function useAdminSubmissions(options?: UseAdminSubmissionsOptions) {
  const {
    autoRefresh = true,
    refreshInterval = 30_000, // 30 seconds
    ...filter
  } = options || {};

  return useQuery({
    queryKey: queryKeys.admin.submissions.list(filter),
    queryFn: () => getApiClient().getSubmissionsAdmin(filter),
    refetchInterval: autoRefresh ? refreshInterval : false,
    refetchIntervalInBackground: false,
  });
}
