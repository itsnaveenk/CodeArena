export type LeaderboardPeriod = 'ALL' | 'WEEK' | 'MONTH';

export interface LeaderboardEntry {
  rank: number;
  userId: number;
  name: string;
  email?: string | null;
  solved: number;
  bestRuntime?: number | null;
}
