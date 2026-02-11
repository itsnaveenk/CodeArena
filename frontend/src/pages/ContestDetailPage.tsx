import { useParams, Link } from 'react-router-dom';
import { useContestBySlug, useContestProblems, useContestRegistration } from '@/hooks/useContests';
import { ContestStatusBadge, ContestTimer, ContestRegistrationButton, ContestProblemList } from '@/components/contest';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Calendar, Users, Clock, Trophy, BarChart3 } from 'lucide-react';

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function ContestDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const { data: contest, isLoading, isError, error } = useContestBySlug(slug || '');
  const { data: problems } = useContestProblems(contest?.id || 0);
  const { data: registration, refetch: refetchRegistration } = useContestRegistration(contest?.id || 0);

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Skeleton className="h-12 w-1/2 mb-4" />
        <Skeleton className="h-6 w-1/4 mb-8" />
        <Skeleton className="h-48 w-full mb-4" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isError || !contest) {
    return (
      <div className="container mx-auto py-8 px-4 text-center">
        <p className="text-destructive mb-4">
          {error instanceof Error ? error.message : 'Contest not found'}
        </p>
        <Button asChild>
          <Link to="/contests">Back to Contests</Link>
        </Button>
      </div>
    );
  }

  const showProblems = contest.status === 'RUNNING' || contest.status === 'FINISHED';
  const showTimer = contest.status === 'RUNNING' && (
    contest.timerMode === 'GLOBAL' || registration?.participantStartTime
  );

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-3xl font-bold">{contest.title}</h1>
            <ContestStatusBadge status={contest.status} />
          </div>
          <p className="text-muted-foreground">
            Created by {contest.createdBy.name}
          </p>
        </div>
        
        <div className="flex items-center gap-4">
          {showTimer && (
            <ContestTimer
              endTime={contest.endTime}
              remainingSeconds={contest.remainingTimeSeconds}
            />
          )}
          <ContestRegistrationButton
            contest={contest}
            registration={registration}
            onSuccess={() => refetchRegistration()}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Description</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="prose dark:prose-invert max-w-none">
              {contest.description || 'No description provided.'}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Contest Info</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <Calendar className="h-5 w-5 text-muted-foreground" />
              <div>
                <div className="text-sm text-muted-foreground">Start Time</div>
                <div>{formatDate(contest.startTime)}</div>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <Calendar className="h-5 w-5 text-muted-foreground" />
              <div>
                <div className="text-sm text-muted-foreground">End Time</div>
                <div>{formatDate(contest.endTime)}</div>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <Clock className="h-5 w-5 text-muted-foreground" />
              <div>
                <div className="text-sm text-muted-foreground">Timer Mode</div>
                <div>{contest.timerMode === 'INDIVIDUAL' ? 'Individual' : 'Global'}</div>
                {contest.durationMinutes && (
                  <div className="text-sm text-muted-foreground">
                    {contest.durationMinutes} minutes
                  </div>
                )}
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <Users className="h-5 w-5 text-muted-foreground" />
              <div>
                <div className="text-sm text-muted-foreground">Participants</div>
                <div>
                  {contest.registeredCount}
                  {contest.maxParticipants && ` / ${contest.maxParticipants}`}
                </div>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <Trophy className="h-5 w-5 text-muted-foreground" />
              <div>
                <div className="text-sm text-muted-foreground">Total Points</div>
                <div>{contest.totalPossiblePoints}</div>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <BarChart3 className="h-5 w-5 text-muted-foreground" />
              <div>
                <div className="text-sm text-muted-foreground">Scoring</div>
                <div>{contest.scoringModel === 'PARTIAL' ? 'Partial' : 'Binary'}</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {showProblems && problems && problems.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Problems</CardTitle>
            <CardDescription>
              {problems.length} problems • {contest.totalPossiblePoints} total points
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ContestProblemList contestSlug={contest.slug} problems={problems} />
          </CardContent>
        </Card>
      )}

      {(contest.status === 'RUNNING' || contest.status === 'FINISHED') && (
        <div className="flex gap-4 mt-6">
          <Button asChild variant="outline">
            <Link to={`/contests/${contest.slug}/leaderboard`}>
              View Leaderboard
            </Link>
          </Button>
          {contest.status === 'FINISHED' && (
            <Button asChild variant="outline">
              <Link to={`/contests/${contest.slug}/results`}>
                View Results
              </Link>
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
