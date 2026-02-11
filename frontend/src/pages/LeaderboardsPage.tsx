import { useMemo, useState } from 'react';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import type { LeaderboardEntry, LeaderboardPeriod } from '@/types';
import { useLeaderboards } from '@/hooks/useLeaderboards';
import { Trophy, Medal, Award, Clock, Target } from 'lucide-react';
import { cn } from '@/lib/utils';

function PeriodLabel(p: LeaderboardPeriod) {
  switch (p) {
    case 'ALL':
      return 'All time';
    case 'WEEK':
      return 'Last 7 days';
    case 'MONTH':
      return 'Last 30 days';
  }
}

function getRankIcon(rank: number) {
  switch (rank) {
    case 1:
      return <Trophy className="h-5 w-5 text-yellow-500" />;
    case 2:
      return <Medal className="h-5 w-5 text-gray-400" />;
    case 3:
      return <Award className="h-5 w-5 text-amber-600" />;
    default:
      return null;
  }
}

function getRankStyle(rank: number) {
  switch (rank) {
    case 1:
      return 'bg-gradient-to-r from-yellow-50 to-yellow-100 dark:from-yellow-950/20 dark:to-yellow-900/20 border-l-4 border-yellow-500';
    case 2:
      return 'bg-gradient-to-r from-gray-50 to-gray-100 dark:from-gray-950/20 dark:to-gray-900/20 border-l-4 border-gray-400';
    case 3:
      return 'bg-gradient-to-r from-amber-50 to-amber-100 dark:from-amber-950/20 dark:to-amber-900/20 border-l-4 border-amber-600';
    default:
      return 'hover:bg-muted/50 transition-colors';
  }
}

function LeaderboardTable({ rows }: { rows: LeaderboardEntry[] }) {
  if (rows.length === 0) {
    return (
      <div className="text-center py-12">
        <Target className="h-16 w-16 mx-auto text-muted-foreground/50 mb-4" />
        <h3 className="text-lg font-medium text-muted-foreground mb-2">No rankings yet</h3>
        <p className="text-sm text-muted-foreground">
          Be the first to solve problems and claim the top spot!
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="text-left border-b-2 border-border">
            <th className="py-4 px-4 font-semibold text-sm text-muted-foreground">#</th>
            <th className="py-4 px-4 font-semibold text-sm text-muted-foreground">User</th>
            <th className="py-4 px-4 font-semibold text-sm text-muted-foreground text-center">Solved</th>
            <th className="py-4 px-4 font-semibold text-sm text-muted-foreground text-right">Best Runtime</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr
              key={r.userId}
              className={cn(
                'border-b last:border-b-0',
                getRankStyle(r.rank)
              )}
            >
              <td className="py-4 px-4">
                <div className="flex items-center gap-2">
                  {getRankIcon(r.rank)}
                  <span className={cn(
                    'font-semibold',
                    r.rank <= 3 ? 'text-lg' : 'text-base'
                  )}>
                    {r.rank}
                  </span>
                </div>
              </td>
              <td className="py-4 px-4">
                <div className="flex items-center gap-3">
                  <div className={cn(
                    'h-10 w-10 rounded-full flex items-center justify-center font-semibold text-sm',
                    r.rank === 1 ? 'bg-yellow-500 text-white' :
                    r.rank === 2 ? 'bg-gray-400 text-white' :
                    r.rank === 3 ? 'bg-amber-600 text-white' :
                    'bg-primary/10 text-primary'
                  )}>
                    {r.name.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div className={cn(
                      'font-semibold',
                      r.rank <= 3 && 'text-base'
                    )}>
                      {r.name}
                    </div>
                    {r.email && (
                      <div className="text-muted-foreground text-xs">{r.email}</div>
                    )}
                  </div>
                </div>
              </td>
              <td className="py-4 px-4 text-center">
                <Badge variant="secondary" className="font-semibold">
                  {r.solved} {r.solved === 1 ? 'problem' : 'problems'}
                </Badge>
              </td>
              <td className="py-4 px-4 text-right">
                <div className="flex items-center justify-end gap-2">
                  <Clock className="h-4 w-4 text-muted-foreground" />
                  <span className="font-mono font-medium">
                    {r.bestRuntime != null ? `${r.bestRuntime.toFixed(3)}s` : '-'}
                  </span>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function LeaderboardsPage() {
  const [period, setPeriod] = useState<LeaderboardPeriod>('ALL');

  const { data, isLoading, error } = useLeaderboards({ period, limit: 50 });

  const rows = useMemo(() => data ?? [], [data]);

  return (
    <>
      <div className="container mx-auto px-4 py-8 space-y-6">
        {/* Header Section */}
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-primary/10 rounded-lg">
                <Trophy className="h-8 w-8 text-primary" />
              </div>
              <div>
                <h1 className="text-3xl font-bold tracking-tight">Leaderboards</h1>
                <p className="text-muted-foreground mt-1">Top solvers by accepted problems</p>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-2 md:items-end">
            <div className="text-sm font-medium text-muted-foreground">Filter by Period</div>
            <Select value={period} onValueChange={(v) => setPeriod(v as LeaderboardPeriod)}>
              <SelectTrigger className="w-full md:w-[220px]">
                <SelectValue placeholder="Select period" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">
                  <div className="flex items-center gap-2">
                    <Trophy className="h-4 w-4" />
                    {PeriodLabel('ALL')}
                  </div>
                </SelectItem>
                <SelectItem value="WEEK">
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4" />
                    {PeriodLabel('WEEK')}
                  </div>
                </SelectItem>
                <SelectItem value="MONTH">
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4" />
                    {PeriodLabel('MONTH')}
                  </div>
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Stats Cards */}
        {!isLoading && !error && rows.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Top Solver
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Trophy className="h-8 w-8 text-yellow-500" />
                  <div>
                    <div className="text-2xl font-bold">{rows[0]?.name}</div>
                    <div className="text-sm text-muted-foreground">
                      {rows[0]?.solved} problems solved
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Total Participants
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Target className="h-8 w-8 text-primary" />
                  <div>
                    <div className="text-2xl font-bold">{rows.length}</div>
                    <div className="text-sm text-muted-foreground">Active solvers</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  Best Runtime
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-3">
                  <Clock className="h-8 w-8 text-green-500" />
                  <div>
                    <div className="text-2xl font-bold font-mono">
                      {rows[0]?.bestRuntime != null
                        ? `${rows[0].bestRuntime.toFixed(3)}s`
                        : '-'}
                    </div>
                    <div className="text-sm text-muted-foreground">Fastest solution</div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Leaderboard Table */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Medal className="h-5 w-5 text-primary" />
              Rankings
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {isLoading ? (
              <div className="space-y-3 p-6">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="flex items-center gap-4">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-4 w-1/3" />
                      <Skeleton className="h-3 w-1/4" />
                    </div>
                    <Skeleton className="h-6 w-20" />
                  </div>
                ))}
              </div>
            ) : error ? (
              <div className="text-center py-12">
                <div className="text-red-600 font-medium mb-2">Failed to load leaderboards</div>
                <p className="text-sm text-muted-foreground">Please try again later</p>
              </div>
            ) : (
              <LeaderboardTable rows={rows} />
            )}
          </CardContent>
        </Card>
      </div>
    </>
  );
}
