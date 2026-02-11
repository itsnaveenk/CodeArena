import type { ContestLeaderboardEntry, ContestProblem } from '@/types/contest';
import { MedalBadge } from './MedalBadge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { cn } from '@/lib/utils';

interface ContestLeaderboardProps {
  entries: ContestLeaderboardEntry[];
  problems?: ContestProblem[];
  isFrozen?: boolean;
  className?: string;
}

function getProblemLabel(index: number): string {
  return String.fromCharCode(65 + index);
}

function formatTime(timeString: string): string {
  if (timeString.includes(':')) return timeString;
  const totalSeconds = parseInt(timeString, 10);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

export function ContestLeaderboard({ entries, problems, isFrozen, className }: ContestLeaderboardProps) {
  const sortedProblems = problems ? [...problems].sort((a, b) => a.displayOrder - b.displayOrder) : [];

  return (
    <div className={className}>
      {isFrozen && (
        <div className="mb-4 p-3 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-200 rounded-lg text-sm">
          🔒 Leaderboard is frozen. Final standings will be revealed after the contest ends.
        </div>
      )}
      
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-16">Rank</TableHead>
            <TableHead>Participant</TableHead>
            <TableHead className="w-24 text-center">Score</TableHead>
            <TableHead className="w-20 text-center">Solved</TableHead>
            <TableHead className="w-24 text-center">Time</TableHead>
            {sortedProblems.map((problem, index) => (
              <TableHead key={problem.problemId} className="w-16 text-center">
                {getProblemLabel(index)}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {entries.map((entry) => (
            <TableRow 
              key={entry.userId}
              className={cn(entry.isCurrentUser && 'bg-primary/5')}
            >
              <TableCell className="font-medium">
                <div className="flex items-center gap-2">
                  {entry.medal && <MedalBadge medal={entry.medal} />}
                  <span>{entry.rank}</span>
                </div>
              </TableCell>
              <TableCell>
                <span className={cn(entry.isCurrentUser && 'font-semibold')}>
                  {entry.username}
                  {entry.isCurrentUser && ' (You)'}
                </span>
              </TableCell>
              <TableCell className="text-center font-medium">
                {entry.totalPoints}
              </TableCell>
              <TableCell className="text-center text-muted-foreground">
                {entry.problemsSolved}
              </TableCell>
              <TableCell className="text-center text-muted-foreground font-mono text-sm">
                {formatTime(entry.totalTime)}
              </TableCell>
              {sortedProblems.map((problem) => {
                const score = entry.problemScores.find(s => s.problemId === problem.problemId);
                const isSolved = score && score.points >= problem.pointValue;
                
                return (
                  <TableCell key={problem.problemId} className="text-center">
                    {score ? (
                      <span className={cn(
                        isSolved ? 'text-green-600 font-medium' : 'text-yellow-600'
                      )}>
                        {score.points}
                      </span>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                );
              })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
