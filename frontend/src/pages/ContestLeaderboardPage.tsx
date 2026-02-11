import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useContestBySlug, useContestLeaderboard, useContestProblems, useMyContestRanking } from '@/hooks/useContests';
import { ContestLeaderboard, ContestStatusBadge } from '@/components/contest';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react';

const PAGE_SIZE = 50;

export default function ContestLeaderboardPage() {
  const { slug } = useParams<{ slug: string }>();
  const [page, setPage] = useState(0);
  
  const { data: contest, isLoading: contestLoading } = useContestBySlug(slug || '');
  const { data: leaderboard, isLoading: leaderboardLoading, refetch } = useContestLeaderboard(
    contest?.id || 0,
    page,
    PAGE_SIZE
  );
  const { data: problems } = useContestProblems(contest?.id || 0);
  const { data: myRanking } = useMyContestRanking(contest?.id || 0);

  const isLoading = contestLoading || leaderboardLoading;

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Skeleton className="h-8 w-1/4 mb-4" />
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

  const isFrozen = contest.status === 'RUNNING' && contest.leaderboardFreezeMinutes > 0;

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Button asChild variant="ghost" size="sm">
            <Link to={`/contests/${slug}`}>
              <ArrowLeft className="h-4 w-4 mr-1" />
              Back
            </Link>
          </Button>
          <h1 className="text-2xl font-bold">{contest.title} - Leaderboard</h1>
          <ContestStatusBadge status={contest.status} />
        </div>
        
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          <RefreshCw className="h-4 w-4 mr-2" />
          Refresh
        </Button>
      </div>

      {myRanking && (
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Your Ranking</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-8">
              <div>
                <div className="text-sm text-muted-foreground">Rank</div>
                <div className="text-2xl font-bold">#{myRanking.rank}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Score</div>
                <div className="text-2xl font-bold">{myRanking.totalPoints}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Problems Solved</div>
                <div className="text-2xl font-bold">{myRanking.problemsSolved}</div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="pt-6">
          {leaderboard && leaderboard.content.length > 0 ? (
            <>
              <ContestLeaderboard
                entries={leaderboard.content}
                problems={problems}
                isFrozen={isFrozen}
              />

              {leaderboard.totalPages > 1 && (
                <div className="flex items-center justify-center gap-4 mt-6">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(page - 1)}
                    disabled={page === 0}
                  >
                    <ChevronLeft className="h-4 w-4 mr-1" />
                    Previous
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    Page {page + 1} of {leaderboard.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(page + 1)}
                    disabled={page >= leaderboard.totalPages - 1}
                  >
                    Next
                    <ChevronRight className="h-4 w-4 ml-1" />
                  </Button>
                </div>
              )}
            </>
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              No participants yet.
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
