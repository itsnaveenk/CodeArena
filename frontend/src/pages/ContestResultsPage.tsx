import { useParams, Link } from 'react-router-dom';
import { useContestBySlug, useContestLeaderboard, useContestProblems, useContestStatistics, useContestEditorials } from '@/hooks/useContests';
import { ContestLeaderboard, ContestStatusBadge, MedalBadge } from '@/components/contest';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Trophy, Users, FileText, BarChart3 } from 'lucide-react';

export default function ContestResultsPage() {
  const { slug } = useParams<{ slug: string }>();
  
  const { data: contest, isLoading: contestLoading } = useContestBySlug(slug || '');
  const { data: leaderboard, isLoading: leaderboardLoading } = useContestLeaderboard(contest?.id || 0, 0, 50);
  const { data: problems } = useContestProblems(contest?.id || 0);
  const { data: statistics } = useContestStatistics(contest?.id || 0);
  const { data: editorials } = useContestEditorials(contest?.id || 0);

  const isLoading = contestLoading || leaderboardLoading;

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Skeleton className="h-8 w-1/4 mb-4" />
        <Skeleton className="h-48 w-full mb-4" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!contest) {
    return (
      <div className="container mx-auto py-8 px-4 text-center">
        <p className="text-destructive mb-4">Contest not found</p>
        <Button asChild>
          <Link to="/contests">Back to Contests</Link>
        </Button>
      </div>
    );
  }

  if (contest.status !== 'FINISHED') {
    return (
      <div className="container mx-auto py-8 px-4 text-center">
        <p className="text-muted-foreground mb-4">Results are not available yet.</p>
        <Button asChild>
          <Link to={`/contests/${slug}`}>Back to Contest</Link>
        </Button>
      </div>
    );
  }

  const topThree = leaderboard?.content.slice(0, 3) || [];

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-center gap-4 mb-6">
        <Button asChild variant="ghost" size="sm">
          <Link to={`/contests/${slug}`}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            Back
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">{contest.title} - Results</h1>
        <ContestStatusBadge status={contest.status} />
      </div>

      {/* Podium */}
      {topThree.length > 0 && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Trophy className="h-5 w-5" />
              Winners
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex justify-center items-end gap-8">
              {topThree[1] && (
                <div className="text-center">
                  <MedalBadge medal="SILVER" className="text-4xl" />
                  <div className="mt-2 font-semibold">{topThree[1].username}</div>
                  <div className="text-sm text-muted-foreground">{topThree[1].totalPoints} pts</div>
                </div>
              )}
              {topThree[0] && (
                <div className="text-center">
                  <MedalBadge medal="GOLD" className="text-5xl" />
                  <div className="mt-2 font-bold text-lg">{topThree[0].username}</div>
                  <div className="text-sm text-muted-foreground">{topThree[0].totalPoints} pts</div>
                </div>
              )}
              {topThree[2] && (
                <div className="text-center">
                  <MedalBadge medal="BRONZE" className="text-4xl" />
                  <div className="mt-2 font-semibold">{topThree[2].username}</div>
                  <div className="text-sm text-muted-foreground">{topThree[2].totalPoints} pts</div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Statistics */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <Users className="h-8 w-8 text-muted-foreground" />
                <div>
                  <div className="text-2xl font-bold">{statistics.totalParticipants}</div>
                  <div className="text-sm text-muted-foreground">Participants</div>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <FileText className="h-8 w-8 text-muted-foreground" />
                <div>
                  <div className="text-2xl font-bold">{statistics.totalSubmissions}</div>
                  <div className="text-sm text-muted-foreground">Submissions</div>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <BarChart3 className="h-8 w-8 text-muted-foreground" />
                <div>
                  <div className="text-2xl font-bold">{statistics.averageScore.toFixed(1)}</div>
                  <div className="text-sm text-muted-foreground">Avg Score</div>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <Trophy className="h-8 w-8 text-muted-foreground" />
                <div>
                  <div className="text-2xl font-bold">{contest.totalPossiblePoints}</div>
                  <div className="text-sm text-muted-foreground">Max Points</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Full Leaderboard */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Final Standings</CardTitle>
          <CardDescription>Complete leaderboard with final rankings</CardDescription>
        </CardHeader>
        <CardContent>
          {leaderboard && leaderboard.content.length > 0 ? (
            <ContestLeaderboard entries={leaderboard.content} problems={problems} />
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              No participants.
            </div>
          )}
        </CardContent>
      </Card>

      {/* Editorials */}
      {editorials && editorials.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Editorials</CardTitle>
            <CardDescription>Problem solutions and explanations</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {editorials.map((editorial) => {
              const problem = problems?.find(p => p.problemId === editorial.problemId);
              return (
                <div key={editorial.id} className="border-b pb-4 last:border-0">
                  <h3 className="font-semibold mb-2">{problem?.title || `Problem ${editorial.problemId}`}</h3>
                  <div className="prose dark:prose-invert max-w-none text-sm">
                    {editorial.content}
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
