import { useQuery } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { useAuthStore } from '@/stores/authStore';
import { StatsCard } from '@/components/dashboard/StatsCard';
import { DifficultyChart } from '@/components/dashboard/DifficultyChart';
import { SubmissionHistory } from '@/components/dashboard/SubmissionHistory';
import { Skeleton } from '@/components/ui/skeleton';
import { CheckCircle, Target, TrendingUp } from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuthStore();
  
  const { data: stats, isLoading } = useQuery({
    queryKey: queryKeys.user.stats(),
    queryFn: () => getApiClient().getUserStats(),
  });

  const acceptanceRate = stats && stats.totalAttempted > 0
    ? ((stats.totalSolved / stats.totalAttempted) * 100).toFixed(1)
    : '0';

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Welcome back, {user?.name}</h1>
        <p className="text-muted-foreground">Track your progress and recent submissions</p>
      </div>

      {isLoading ? (
        <div className="grid gap-6 md:grid-cols-3 mb-8">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-32 w-full" />
          ))}
        </div>
      ) : stats ? (
        <>
          <div className="grid gap-6 md:grid-cols-3 mb-8">
            <StatsCard
              title="Problems Solved"
              value={stats.totalSolved}
              description={`Out of ${stats.totalAttempted} attempted`}
              icon={CheckCircle}
            />
            <StatsCard
              title="Acceptance Rate"
              value={`${acceptanceRate}%`}
              description="Based on all submissions"
              icon={TrendingUp}
            />
            <StatsCard
              title="Total Attempted"
              value={stats.totalAttempted}
              description="Keep going!"
              icon={Target}
            />
          </div>

          <div className="grid gap-6 lg:grid-cols-3 mb-8">
            <div className="lg:col-span-1">
              <DifficultyChart stats={stats} />
            </div>
            <div className="lg:col-span-2">
              <SubmissionHistory />
            </div>
          </div>
        </>
      ) : (
        <div className="text-center py-12 text-muted-foreground">
          <p>Unable to load stats. Please try again later.</p>
        </div>
      )}
    </div>
  );
}
