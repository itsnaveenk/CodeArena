import { Link } from 'react-router-dom';
import type { ProblemListItem } from '@/types';
import { DifficultyBadge } from './DifficultyBadge';
import { SolveStatusIndicator } from './SolveStatusIndicator';
import { Badge } from '@/components/ui/badge';

interface ProblemCardProps {
  problem: ProblemListItem;
}

export function ProblemCard({ problem }: ProblemCardProps) {
  return (
    <Link
      to={`/problems/${problem.slug}`}
      className="block p-4 border rounded-lg hover:bg-muted/50 transition-colors"
    >
      <div className="flex items-center gap-4">
        <SolveStatusIndicator status={problem.solveStatus} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 mb-1">
            <span className="text-sm text-muted-foreground">#{problem.id}</span>
            <h3 className="font-medium truncate">{problem.title}</h3>
          </div>
          {problem.tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {problem.tags.slice(0, 3).map((tag) => (
                <Badge key={tag} variant="secondary" className="text-xs">
                  {tag}
                </Badge>
              ))}
              {problem.tags.length > 3 && (
                <Badge variant="secondary" className="text-xs">
                  +{problem.tags.length - 3}
                </Badge>
              )}
            </div>
          )}
        </div>
        <DifficultyBadge difficulty={problem.difficulty} />
      </div>
    </Link>
  );
}
