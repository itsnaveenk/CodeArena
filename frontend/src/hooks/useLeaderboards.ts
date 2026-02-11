import { useQuery } from '@tanstack/react-query';

import { getApiClient } from '@/lib/api';
import type { LeaderboardPeriod } from '@/types';

export function useLeaderboards(params?: { period?: LeaderboardPeriod; limit?: number }) {
  return useQuery({
    queryKey: ['leaderboards', params ?? {}],
    queryFn: () => getApiClient().getLeaderboards(params),
  });
}
