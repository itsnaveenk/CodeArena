import type { ProblemDetail } from '@/types';
import { DifficultyBadge } from './DifficultyBadge';
import { Badge } from '@/components/ui/badge';

interface ProblemDescriptionProps {
  problem: ProblemDetail;
}

export function ProblemDescription({ problem }: ProblemDescriptionProps) {
  return (
    <div className="space-y-6">
      <div>
        <div className="flex items-center gap-3 mb-2">
          <h1 className="text-2xl font-bold">{problem.title}</h1>
          <DifficultyBadge difficulty={problem.difficulty} />
        </div>
        {problem.tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {problem.tags.map((tag) => (
              <Badge key={tag} variant="secondary" className="text-xs">
                {tag}
              </Badge>
            ))}
          </div>
        )}
      </div>

      <div className="prose prose-sm dark:prose-invert max-w-none">
        <div dangerouslySetInnerHTML={{ __html: formatMarkdown(problem.statement) }} />
      </div>

      {problem.constraints && (
        <div>
          <h3 className="font-semibold mb-2">Constraints</h3>
          <div className="text-sm text-muted-foreground whitespace-pre-wrap font-mono bg-muted p-3 rounded">
            {problem.constraints}
          </div>
        </div>
      )}
    </div>
  );
}

function formatMarkdown(text: string): string {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code class="bg-muted px-1 rounded">$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br/>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>');
}
