import { useAdminStats } from '@/hooks/useAdminStats';
import { StatCard } from './StatCard';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Users,
  FileText,
  Activity,
  CheckCircle,
  Clock,
  XCircle,
  Archive,
  UserCog,
  Shield,
} from 'lucide-react';

export function AdminOverview() {
  const { data: stats, isLoading, error } = useAdminStats();

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-destructive">Failed to load statistics</p>
        <p className="text-sm text-muted-foreground mt-1">
          {error instanceof Error ? error.message : 'Unknown error'}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Main Stats */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Users"
          value={stats?.totalUsers ?? 0}
          icon={Users}
          isLoading={isLoading}
        />
        <StatCard
          title="Total Problems"
          value={stats?.totalProblems ?? 0}
          icon={FileText}
          isLoading={isLoading}
        />
        <StatCard
          title="Total Submissions"
          value={stats?.totalSubmissions ?? 0}
          icon={Activity}
          isLoading={isLoading}
        />
        <StatCard
          title="Submissions Today"
          value={stats?.submissionsToday ?? 0}
          icon={Activity}
          description="Last 24 hours"
          isLoading={isLoading}
        />
      </div>

      {/* Breakdown Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {/* Users by Role */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Users by Role</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm">Users</span>
                  </div>
                  <span className="font-medium">{stats?.usersByRole?.USER ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <UserCog className="h-4 w-4 text-blue-500" />
                    <span className="text-sm">Problem Setters</span>
                  </div>
                  <span className="font-medium">{stats?.usersByRole?.PROBLEM_SETTER ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Shield className="h-4 w-4 text-red-500" />
                    <span className="text-sm">Admins</span>
                  </div>
                  <span className="font-medium">{stats?.usersByRole?.ADMIN ?? 0}</span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Problems by Status */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Problems by Status</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <CheckCircle className="h-4 w-4 text-green-500" />
                    <span className="text-sm">Published</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByStatus?.PUBLISHED ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4 text-yellow-500" />
                    <span className="text-sm">Pending Review</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByStatus?.PENDING_REVIEW ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <FileText className="h-4 w-4 text-gray-500" />
                    <span className="text-sm">Draft</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByStatus?.DRAFT ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <XCircle className="h-4 w-4 text-red-500" />
                    <span className="text-sm">Rejected</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByStatus?.REJECTED ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Archive className="h-4 w-4 text-slate-500" />
                    <span className="text-sm">Archived</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByStatus?.ARCHIVED ?? 0}</span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Problems by Difficulty */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Problems by Difficulty</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full bg-green-500" />
                    <span className="text-sm">Easy</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByDifficulty?.EASY ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full bg-yellow-500" />
                    <span className="text-sm">Medium</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByDifficulty?.MEDIUM ?? 0}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full bg-red-500" />
                    <span className="text-sm">Hard</span>
                  </div>
                  <span className="font-medium">{stats?.problemsByDifficulty?.HARD ?? 0}</span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Acceptance Rate */}
      {stats && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Platform Performance</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <div>
                <div className="text-2xl font-bold">
                  {(stats.acceptanceRate * 100).toFixed(1)}%
                </div>
                <div className="text-sm text-muted-foreground">
                  Overall Acceptance Rate
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

export default AdminOverview;
