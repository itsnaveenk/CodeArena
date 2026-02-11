import { Link } from 'react-router-dom';
import { CheckCircle, Circle, Trophy } from 'lucide-react';
import type { ContestProblem } from '@/types/contest';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

interface ContestProblemListProps {
  contestSlug: string;
  problems: ContestProblem[];
}

function getProblemLabel(index: number): string {
  return String.fromCharCode(65 + index); // A, B, C, ...
}

export function ContestProblemList({ contestSlug, problems }: ContestProblemListProps) {
  const sortedProblems = [...problems].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-16">#</TableHead>
          <TableHead>Problem</TableHead>
          <TableHead className="w-24 text-center">Points</TableHead>
          <TableHead className="w-24 text-center">Solved</TableHead>
          <TableHead className="w-24 text-center">Your Score</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sortedProblems.map((problem, index) => {
          const isSolved = problem.userBestPoints !== undefined && problem.userBestPoints >= problem.pointValue;
          const hasAttempted = problem.userSubmissionCount > 0;
          
          return (
            <TableRow key={problem.id}>
              <TableCell className="font-medium">
                {getProblemLabel(index)}
              </TableCell>
              <TableCell>
                <Link
                  to={`/contests/${contestSlug}/problems/${problem.problemId}`}
                  className="hover:underline flex items-center gap-2"
                >
                  {isSolved ? (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  ) : hasAttempted ? (
                    <Circle className="h-4 w-4 text-yellow-500" />
                  ) : null}
                  {problem.title}
                </Link>
              </TableCell>
              <TableCell className="text-center">
                <div className="flex items-center justify-center gap-1">
                  <Trophy className="h-4 w-4 text-muted-foreground" />
                  {problem.pointValue}
                </div>
              </TableCell>
              <TableCell className="text-center text-muted-foreground">
                {problem.solveCount}
              </TableCell>
              <TableCell className="text-center">
                {problem.userBestPoints !== undefined ? (
                  <span className={isSolved ? 'text-green-600 font-medium' : 'text-yellow-600'}>
                    {problem.userBestPoints}
                  </span>
                ) : (
                  <span className="text-muted-foreground">-</span>
                )}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
