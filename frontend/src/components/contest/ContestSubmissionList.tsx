import type { ContestSubmission } from '@/types/contest';
import type { Verdict } from '@/types';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { Trophy } from 'lucide-react';

interface ContestSubmissionListProps {
  submissions: ContestSubmission[];
  bestSubmissionId?: string;
  className?: string;
}

const VERDICT_COLORS: Record<Verdict, string> = {
  ACCEPTED: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  WRONG_ANSWER: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  TLE: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  RUNTIME_ERROR: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400',
  COMPILATION_ERROR: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400',
};

function getLanguageName(languageId: number): string {
  const map: Record<number, string> = {
    50: 'C', 54: 'C++', 62: 'Java', 63: 'JavaScript',
    71: 'Python 3', 72: 'Ruby', 73: 'Rust', 74: 'TypeScript',
  };
  return map[languageId] || `Lang ${languageId}`;
}

export function ContestSubmissionList({ submissions, bestSubmissionId, className }: ContestSubmissionListProps) {
  if (!submissions.length) {
    return (
      <p className={cn('text-muted-foreground text-center py-4', className)}>
        No submissions yet.
      </p>
    );
  }

  const bestId = bestSubmissionId ?? submissions.reduce(
    (best, s) => (s.pointsEarned > (best?.pointsEarned ?? -1) ? s : best), submissions[0]
  )?.id;

  return (
    <div className={className}>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Time</TableHead>
            <TableHead>Language</TableHead>
            <TableHead>Verdict</TableHead>
            <TableHead className="text-center">Tests</TableHead>
            <TableHead className="text-center">Points</TableHead>
            <TableHead className="text-center">Runtime</TableHead>
            <TableHead className="text-center">Memory</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {submissions.map((s) => {
            const isBest = s.id === bestId;
            return (
              <TableRow key={s.id} className={cn(isBest && 'bg-primary/5')}>
                <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
                  {new Date(s.submittedAt).toLocaleTimeString()}
                  {'isAutoSubmitted' in s && (s as { isAutoSubmitted?: boolean }).isAutoSubmitted && (
                    <Badge variant="outline" className="ml-1 text-xs">auto</Badge>
                  )}
                </TableCell>
                <TableCell className="text-sm">
                  {getLanguageName(s.languageId)}
                </TableCell>
                <TableCell>
                  <Badge className={VERDICT_COLORS[s.verdict]}>
                    {s.verdict.replace(/_/g, ' ')}
                  </Badge>
                </TableCell>
                <TableCell className="text-center text-muted-foreground">
                  {s.passedTestcases}/{s.totalTestcases}
                </TableCell>
                <TableCell className="text-center font-medium">
                  <div className="flex items-center justify-center gap-1">
                    {isBest && <Trophy className="h-3 w-3 text-yellow-500" />}
                    {s.pointsEarned}
                  </div>
                </TableCell>
                <TableCell className="text-center text-muted-foreground text-sm">
                  {s.runtime != null ? `${s.runtime}s` : '-'}
                </TableCell>
                <TableCell className="text-center text-muted-foreground text-sm">
                  {s.memory != null ? `${(s.memory / 1024).toFixed(1)}MB` : '-'}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
